package io.attestry.workflow.application.claim;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.claim.command.CompleteClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.PresignClaimEvidenceCommand;
import io.attestry.workflow.application.claim.command.SubmitPurchaseClaimCommand;
import io.attestry.workflow.application.claim.result.ClaimEvidenceView;
import io.attestry.workflow.application.claim.result.MyClaimView;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.port.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.usecase.PurchaseClaimSubmitUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseClaimSubmitService implements PurchaseClaimSubmitUseCase {
    private static final Duration DOWNLOAD_TTL = Duration.ofDays(3);
    private final PurchaseClaimRepository purchaseClaimRepository;
    private final WorkflowEvidencePort evidencePort;
    private final PurchaseClaimEvidenceService evidenceService;
    private final ObjectStoragePort objectStoragePort;
    private final Clock clock;

    public PurchaseClaimSubmitService(
        PurchaseClaimRepository purchaseClaimRepository,
        WorkflowEvidencePort evidencePort,
        PurchaseClaimEvidenceService evidenceService,
        ObjectStoragePort objectStoragePort,
        Clock clock
    ) {
        this.purchaseClaimRepository = purchaseClaimRepository;
        this.evidencePort = evidencePort;
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

        List<String> evidenceHashes = evidencePort.findReadyEvidenceHashes(command.evidenceGroupId());
        if (evidenceHashes.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.CLAIM_EVIDENCE_INSUFFICIENT, "At least one READY evidence is required");
        }

        Instant now = Instant.now(clock);
        PurchaseClaim claim = PurchaseClaim.submit(
            UUID.randomUUID().toString(),
            principal.userId(),
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
    public PresignedEvidenceUploadResult presignEvidence(AuthPrincipal principal, PresignClaimEvidenceCommand command) {
        return evidenceService.presignEvidence(principal, command);
    }

    @Override
    @Transactional
    public EvidenceCompleteResult completeEvidence(AuthPrincipal principal, CompleteClaimEvidenceCommand command) {
        return evidenceService.completeEvidence(principal, command);
    }

    private ClaimEvidenceView toEvidenceView(WorkflowEvidencePort.EvidenceView evidence) {
        ObjectStoragePort.PresignedDownload download = objectStoragePort.issuePresignedDownload(
            evidence.objectKey(),
            DOWNLOAD_TTL
        );
        return new ClaimEvidenceView(
            evidence.evidenceId(),
            evidence.status(),
            download.downloadUrl(),
            download.expiresAt()
        );
    }

    private void assertEvidenceGroupOwnedByClaimant(String evidenceGroupId, String claimantUserId) {
        if (evidenceGroupId == null || evidenceGroupId.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "evidenceGroupId is required");
        }
        WorkflowEvidencePort.EvidenceGroupScopeView scope = evidencePort.findEvidenceGroupScope(evidenceGroupId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence group not found"));
        if (scope.ownerUserId() == null || !scope.ownerUserId().equals(claimantUserId)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Evidence group is not owned by claimant"
            );
        }
    }

    private List<ClaimEvidenceView> resolveClaimEvidences(String evidenceGroupId) {
        return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(e -> "READY".equalsIgnoreCase(e.status()))
            .map(this::toEvidenceView)
            .toList();
    }
}
