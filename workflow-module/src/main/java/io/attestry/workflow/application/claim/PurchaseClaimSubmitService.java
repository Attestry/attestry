package io.attestry.workflow.application.claim;

import io.attestry.userauth.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ClaimEvidenceView;
import io.attestry.workflow.application.claim.result.MyClaimView;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedShipmentEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceCompleteResult;
import io.attestry.workflow.application.usecase.PurchaseClaimSubmitUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseClaimSubmitService implements PurchaseClaimSubmitUseCase {
    private static final Duration DOWNLOAD_TTL = Duration.ofDays(3);
    private static final Set<String> BRAND_PROFILE_SCOPES = Set.of(
        "SCOPE_BRAND_MINT",
        "BRAND_MINT"
    );

    private final PurchaseClaimRepository purchaseClaimRepository;
    private final ShipmentEvidencePort shipmentEvidencePort;
    private final PurchaseClaimEvidenceService evidenceService;
    private final ObjectStoragePort objectStoragePort;
    private final Clock clock;

    public PurchaseClaimSubmitService(
        PurchaseClaimRepository purchaseClaimRepository,
        ShipmentEvidencePort shipmentEvidencePort,
        PurchaseClaimEvidenceService evidenceService,
        ObjectStoragePort objectStoragePort,
        Clock clock
    ) {
        this.purchaseClaimRepository = purchaseClaimRepository;
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.evidenceService = evidenceService;
        this.objectStoragePort = objectStoragePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmitPurchaseClaimResult submit(AuthPrincipal principal, SubmitPurchaseClaimCommand command) {
        if (principal.userId() == null || principal.userId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Authenticated user is required");
        }
        assertEvidenceGroupOwnedByClaimant(command.evidenceGroupId(), principal.userId());

        List<String> evidenceHashes = shipmentEvidencePort.findReadyEvidenceHashes(command.evidenceGroupId());
        if (evidenceHashes.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.CLAIM_EVIDENCE_INSUFFICIENT, "At least one READY evidence is required");
        }

        Instant now = Instant.now(clock);
        PurchaseClaim claim = PurchaseClaim.submit(
            UUID.randomUUID().toString(),
            principal.userId(),
            resolveSubmitterProfileType(principal),
            command.serialNumber(),
            command.modelName(),
            command.evidenceGroupId(),
            command.note(),
            now
        );

        PurchaseClaim saved = purchaseClaimRepository.save(claim);
        return new SubmitPurchaseClaimResult(saved.claimId(), saved.status().name(), saved.submittedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyClaimView> listMyClaims(AuthPrincipal principal) {
        return purchaseClaimRepository.findByClaimantUserId(principal.userId()).stream()
            .map(c -> new MyClaimView(
                c.claimId(),
                c.submitterProfileType(),
                c.serialNumber(), c.modelName(),
                c.status().name(), c.submittedAt(),
                c.rejectionReason(), c.passportId(), c.assetId(),
                resolveClaimEvidences(c.evidenceGroupId())
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimEvidenceView> listMyClaimEvidences(AuthPrincipal principal, String claimId) {
        PurchaseClaim claim = purchaseClaimRepository.findById(claimId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.CLAIM_NOT_FOUND, "Purchase claim not found"));

        if (!claim.claimantUserId().equals(principal.userId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.FORBIDDEN_SCOPE, "Only claimant can access evidences");
        }

        return resolveClaimEvidences(claim.evidenceGroupId());
    }

    @Override
    @Transactional
    public PresignedShipmentEvidenceUploadResult presignEvidence(AuthPrincipal principal, PresignClaimEvidenceCommand command) {
        return evidenceService.presignEvidence(principal, command);
    }

    @Override
    @Transactional
    public ShipmentEvidenceCompleteResult completeEvidence(AuthPrincipal principal, CompleteClaimEvidenceCommand command) {
        return evidenceService.completeEvidence(principal, command);
    }

    private String resolveSubmitterProfileType(AuthPrincipal principal) {
        if (principal.scopes() != null && principal.scopes().stream().anyMatch(BRAND_PROFILE_SCOPES::contains)) {
            return "BRAND";
        }
        return "OWNER";
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

    private void assertEvidenceGroupOwnedByClaimant(String evidenceGroupId, String claimantUserId) {
        if (evidenceGroupId == null || evidenceGroupId.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "evidenceGroupId is required");
        }
        ShipmentEvidencePort.EvidenceGroupScopeView scope = shipmentEvidencePort.findEvidenceGroupScope(evidenceGroupId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence group not found"));
        if (scope.ownerUserId() == null || !scope.ownerUserId().equals(claimantUserId)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Evidence group is not owned by claimant"
            );
        }
    }

    private List<ClaimEvidenceView> resolveClaimEvidences(String evidenceGroupId) {
        return shipmentEvidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(e -> "READY".equalsIgnoreCase(e.status()))
            .map(this::toEvidenceView)
            .toList();
    }
}
