package io.attestry.workflow.application.claim.command;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import io.attestry.workflow.application.claim.internal.PurchaseClaimEvidenceViewResolver;
import io.attestry.workflow.application.claim.internal.PurchaseClaimLookupService;
import io.attestry.workflow.application.claim.view.ClaimEvidenceView;
import io.attestry.workflow.application.claim.view.PendingClaimView;
import io.attestry.workflow.application.port.claim.PurchaseClaimProductMintPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.transfer.TransferOwnershipUpdatePort;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.event.WorkflowLedgerEvents;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseClaimAdminService implements PurchaseClaimAdminUseCase {
    private final PurchaseClaimRepository purchaseClaimRepository;
    private final PurchaseClaimProductMintPort productMintPort;
    private final TransferOwnershipUpdatePort ownershipUpdatePort;
    private final WorkflowLedgerOutboxPort ledgerOutboxPort;
    private final WorkflowEvidencePort evidencePort;
    private final PurchaseClaimLookupService claimLookupService;
    private final PurchaseClaimEvidenceViewResolver evidenceViewResolver;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;


    @Override
    @Transactional(readOnly = true)
    public List<PendingClaimView> listPendingClaims(WorkflowActorContext principal) {
        authorizationSupport.assertLivePermission(principal, principal.tenantId(), PermissionCodes.PLATFORM_ADMIN, "purchase-claim:list");
        return purchaseClaimRepository.findByStatus(PurchaseClaimStatus.SUBMITTED).stream()
            .map(c -> new PendingClaimView(
                c.claimId(), c.claimantUserId(),
                c.serialNumber(), c.modelName(),
                c.evidenceGroupId(), c.note(),
                c.status().name(), c.submittedAt()
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimEvidenceView> listClaimEvidences(WorkflowActorContext principal, String claimId) {
        authorizationSupport.assertLivePermission(principal, principal.tenantId(), PermissionCodes.PLATFORM_ADMIN, "purchase-claim:evidences:" + claimId);
        PurchaseClaim claim = claimLookupService.getSubmitted(claimId);
        return evidenceViewResolver.resolveReadyEvidenceViews(claim.evidenceGroupId());
    }

    @Override
    @Transactional
    public ApprovePurchaseClaimResult approve(
        WorkflowActorContext principal,
        String claimId,
        ApprovePurchaseClaimCommand command
    ) {
        authorizationSupport.assertLivePermission(principal, principal.tenantId(), PermissionCodes.PLATFORM_ADMIN, "purchase-claim:approve:" + claimId);

        PurchaseClaim claim = claimLookupService.getSubmitted(claimId);

        PurchaseClaimProductMintPort.MintResult mintResult = productMintPort.mint(
            new PurchaseClaimProductMintPort.MintRequest(
                principal.userId(),
                principal.tenantId(),
                principal.scopes(),
                principal.scopes() != null && principal.scopes().contains(PermissionCodes.PLATFORM_ADMIN),
                principal.tenantId(),
                claim.serialNumber(),
                claim.modelName(),
                command.manufacturedAt(),
                command.productionBatch(),
                command.factoryCode()
            )
        );

        Instant now = Instant.now(clock);
        ownershipUpdatePort.upsertOwner(mintResult.passportId(), claim.claimantUserId(), now);

        PurchaseClaim approved = claim.approve(principal.userId(), mintResult.passportId(), mintResult.assetId(), now);
        purchaseClaimRepository.save(approved);

        List<String> evidenceHashes = evidencePort.findReadyEvidenceHashes(claim.evidenceGroupId());
        ledgerOutboxPort.enqueue(
            WorkflowLedgerEvents.purchaseClaimApproved(
                approved,
                evidenceHashes,
                "ADMIN",
                principal.userId()
            )
        );

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
        WorkflowActorContext principal,
        String claimId,
        String reason
    ) {
        authorizationSupport.assertLivePermission(principal, principal.tenantId(), PermissionCodes.PLATFORM_ADMIN, "purchase-claim:reject:" + claimId);

        PurchaseClaim claim = claimLookupService.getSubmitted(claimId);

        Instant now = Instant.now(clock);
        PurchaseClaim rejected = claim.reject(principal.userId(), reason, now);
        purchaseClaimRepository.save(rejected);

        return new RejectPurchaseClaimResult(rejected.claimId(), rejected.status().name(), rejected.rejectionReason());
    }
}
