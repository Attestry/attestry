package io.attestry.workflow.application.claim;

import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.application.usecase.ProductMintUseCase.MintProductCommand;
import io.attestry.product.application.usecase.ProductMintUseCase.MintedProductResult;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.ApprovePurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.PendingClaimView;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.port.ShipmentLedgerOutboxPort;
import io.attestry.workflow.application.port.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.PurchaseClaimAdminUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseClaimAdminService implements PurchaseClaimAdminUseCase {

    private final PurchaseClaimRepository purchaseClaimRepository;
    private final ProductMintUseCase productMintUseCase;
    private final TransferOwnershipUpdatePort ownershipUpdatePort;
    private final ShipmentLedgerOutboxPort ledgerOutboxPort;
    private final ShipmentEvidencePort shipmentEvidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;

    public PurchaseClaimAdminService(
        PurchaseClaimRepository purchaseClaimRepository,
        ProductMintUseCase productMintUseCase,
        TransferOwnershipUpdatePort ownershipUpdatePort,
        ShipmentLedgerOutboxPort ledgerOutboxPort,
        ShipmentEvidencePort shipmentEvidencePort,
        WorkflowAuthorizationSupport authorizationSupport,
        Clock clock
    ) {
        this.purchaseClaimRepository = purchaseClaimRepository;
        this.productMintUseCase = productMintUseCase;
        this.ownershipUpdatePort = ownershipUpdatePort;
        this.ledgerOutboxPort = ledgerOutboxPort;
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.authorizationSupport = authorizationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingClaimView> listPendingClaims(AuthPrincipal principal, String tenantId, String groupId) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.PURCHASE_CLAIM_APPROVE, "purchase-claim:list");

        return purchaseClaimRepository.findByTenantIdAndGroupIdAndStatus(tenantId, groupId, PurchaseClaimStatus.SUBMITTED)
            .stream()
            .map(c -> new PendingClaimView(
                c.claimId(), c.claimantUserId(),
                c.serialNumber(), c.modelName(),
                c.evidenceGroupId(), c.note(),
                c.status().name(), c.submittedAt()
            ))
            .toList();
    }

    @Override
    @Transactional
    public ApprovePurchaseClaimResult approve(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        String claimId,
        ApprovePurchaseClaimCommand command
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.PURCHASE_CLAIM_APPROVE, "purchase-claim:approve:" + claimId);

        PurchaseClaim claim = findAndValidate(claimId, tenantId, groupId);

        MintedProductResult mintResult = productMintUseCase.mint(
            ActorContext.from(principal),
            new MintProductCommand(
                tenantId, groupId,
                claim.serialNumber(), null, claim.modelName(),
                command.manufacturedAt(), command.productionBatch(), command.factoryCode(),
                null
            )
        );

        Instant now = Instant.now(clock);
        ownershipUpdatePort.upsertOwner(mintResult.passportId(), claim.claimantUserId(), now);

        PurchaseClaim approved = claim.approve(principal.userId(), mintResult.passportId(), mintResult.assetId(), now);
        purchaseClaimRepository.save(approved);

        List<String> evidenceHashes = shipmentEvidencePort.findReadyEvidenceHashes(claim.evidenceGroupId());
        ledgerOutboxPort.enqueue(WorkflowLedgerEventEnvelope.purchaseClaimApproved(approved, evidenceHashes));

        return new ApprovePurchaseClaimResult(
            approved.claimId(),
            approved.passportId(),
            approved.assetId(),
            mintResult.qrPublicCode(),
            approved.status().name()
        );
    }

    @Override
    @Transactional
    public RejectPurchaseClaimResult reject(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        String claimId,
        String reason
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.PURCHASE_CLAIM_APPROVE, "purchase-claim:reject:" + claimId);

        PurchaseClaim claim = findAndValidate(claimId, tenantId, groupId);

        Instant now = Instant.now(clock);
        PurchaseClaim rejected = claim.reject(principal.userId(), reason, now);
        purchaseClaimRepository.save(rejected);

        return new RejectPurchaseClaimResult(rejected.claimId(), rejected.status().name(), rejected.rejectionReason());
    }

    private PurchaseClaim findAndValidate(String claimId, String tenantId, String groupId) {
        PurchaseClaim claim = purchaseClaimRepository.findById(claimId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.CLAIM_NOT_FOUND, "Purchase claim not found"));

        if (!tenantId.equals(claim.tenantId()) || !groupId.equals(claim.groupId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Claim does not belong to tenant/group");
        }

        if (claim.status() != PurchaseClaimStatus.SUBMITTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.CLAIM_INVALID_STATE, "Claim is not in SUBMITTED state");
        }

        return claim;
    }
}
