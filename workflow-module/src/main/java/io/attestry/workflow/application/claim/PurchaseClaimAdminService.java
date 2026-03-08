package io.attestry.workflow.application.claim;

import io.attestry.product.application.usecase.ProductMintUseCase;
import io.attestry.product.application.usecase.ProductMintUseCase.MintProductCommand;
import io.attestry.product.application.usecase.ProductMintUseCase.MintedProductResult;
import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.ApprovePurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ApprovePurchaseClaimResult;
import io.attestry.workflow.application.claim.result.ClaimEvidenceView;
import io.attestry.workflow.application.claim.result.PendingClaimView;
import io.attestry.workflow.application.claim.result.RejectPurchaseClaimResult;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.port.WorkflowLedgerOutboxPort;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseClaimAdminService implements PurchaseClaimAdminUseCase {
    private static final Duration DOWNLOAD_TTL = Duration.ofDays(3);

    private final PurchaseClaimRepository purchaseClaimRepository;
    private final ProductMintUseCase productMintUseCase;
    private final TransferOwnershipUpdatePort ownershipUpdatePort;
    private final WorkflowLedgerOutboxPort ledgerOutboxPort;
    private final ShipmentEvidencePort shipmentEvidencePort;
    private final ObjectStoragePort objectStoragePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final Clock clock;

    public PurchaseClaimAdminService(
        PurchaseClaimRepository purchaseClaimRepository,
        ProductMintUseCase productMintUseCase,
        TransferOwnershipUpdatePort ownershipUpdatePort,
        WorkflowLedgerOutboxPort ledgerOutboxPort,
        ShipmentEvidencePort shipmentEvidencePort,
        ObjectStoragePort objectStoragePort,
        WorkflowAuthorizationSupport authorizationSupport,
        Clock clock
    ) {
        this.purchaseClaimRepository = purchaseClaimRepository;
        this.productMintUseCase = productMintUseCase;
        this.ownershipUpdatePort = ownershipUpdatePort;
        this.ledgerOutboxPort = ledgerOutboxPort;
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.objectStoragePort = objectStoragePort;
        this.authorizationSupport = authorizationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingClaimView> listPendingClaims(AuthPrincipal principal) {
        authorizationSupport.assertLivePermission(principal, principal.tenantId(), PermissionCodes.PURCHASE_CLAIM_APPROVE, "purchase-claim:list");
        List<PurchaseClaim> pendingClaims = purchaseClaimRepository.findByStatus(PurchaseClaimStatus.SUBMITTED);
        return toPendingViews(pendingClaims);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimEvidenceView> listClaimEvidences(AuthPrincipal principal, String claimId) {
        PurchaseClaim claim = purchaseClaimRepository.findById(claimId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.CLAIM_NOT_FOUND, "Purchase claim not found"));

        authorizationSupport.assertLivePermission(
            principal,
            principal.tenantId(),
            PermissionCodes.PURCHASE_CLAIM_APPROVE,
            "purchase-claim:evidences:" + claimId
        );

        return shipmentEvidencePort.findEvidenceByEvidenceGroupId(claim.evidenceGroupId()).stream()
            .filter(e -> "READY".equalsIgnoreCase(e.status()))
            .map(this::toEvidenceView)
            .toList();
    }

    private List<PendingClaimView> toPendingViews(List<PurchaseClaim> pendingClaims) {
        return pendingClaims
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
        String claimId,
        ApprovePurchaseClaimCommand command
    ) {
        authorizationSupport.assertLivePermission(principal, principal.tenantId(), PermissionCodes.PURCHASE_CLAIM_APPROVE, "purchase-claim:approve:" + claimId);

        PurchaseClaim claim = findSubmittedClaim(claimId);
        String targetTenantId = resolveClaimTenantId(claim.evidenceGroupId());
        String safeModelName = (claim.modelName() == null || claim.modelName().isBlank())
            ? claim.serialNumber()
            : claim.modelName();
        MintedProductResult mintResult;
        try {
            mintResult = productMintUseCase.mint(
                ActorContext.from(principal),
                new MintProductCommand(
                    targetTenantId,
                    claim.serialNumber(), null, safeModelName,
                    command.manufacturedAt(), command.productionBatch(), command.factoryCode(),
                    null
                )
            );
        } catch (RuntimeException ex) {
            if (containsTenantForeignKeyViolation(ex)) {
                throw new WorkflowDomainException(
                    WorkflowErrorCode.INVALID_REQUEST,
                    "신청 데이터의 tenant가 유효하지 않아 승인할 수 없습니다. 반려 처리 후 재신청이 필요합니다."
                );
            }
            throw ex;
        }

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
        String claimId,
        String reason
    ) {
        authorizationSupport.assertLivePermission(principal, principal.tenantId(), PermissionCodes.PURCHASE_CLAIM_APPROVE, "purchase-claim:reject:" + claimId);

        PurchaseClaim claim = findSubmittedClaim(claimId);

        Instant now = Instant.now(clock);
        PurchaseClaim rejected = claim.reject(principal.userId(), reason, now);
        purchaseClaimRepository.save(rejected);

        return new RejectPurchaseClaimResult(rejected.claimId(), rejected.status().name(), rejected.rejectionReason());
    }

    private PurchaseClaim findSubmittedClaim(String claimId) {
        PurchaseClaim claim = purchaseClaimRepository.findById(claimId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.CLAIM_NOT_FOUND, "Purchase claim not found"));

        if (claim.status() != PurchaseClaimStatus.SUBMITTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.CLAIM_INVALID_STATE, "Claim is not in SUBMITTED state");
        }

        return claim;
    }

    private boolean containsTenantForeignKeyViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("fk_product_assets_tenant")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String resolveClaimTenantId(String evidenceGroupId) {
        ShipmentEvidencePort.EvidenceGroupScopeView scope = shipmentEvidencePort.findEvidenceGroupScope(evidenceGroupId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence group not found"));
        if (scope.tenantId() == null || scope.tenantId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Evidence group tenant context is missing");
        }
        return scope.tenantId();
    }

    private ClaimEvidenceView toEvidenceView(ShipmentEvidencePort.ShipmentEvidenceView evidence) {
        try {
            ObjectStoragePort.PresignedDownload download = objectStoragePort.issuePresignedDownload(
                evidence.objectKey(),
                DOWNLOAD_TTL
            );
            return new ClaimEvidenceView(
                evidence.evidenceId(),
                evidence.status(),
                download.downloadUrl(),
                download.expiresAt(),
                null,
                null
            );
        } catch (RuntimeException ex) {
            return new ClaimEvidenceView(
                evidence.evidenceId(),
                "PRESIGN_FAILED",
                null,
                null,
                "EVIDENCE_PRESIGN_FAILED",
                "첨부 파일 다운로드 링크 생성에 실패했습니다."
            );
        }
    }
}
