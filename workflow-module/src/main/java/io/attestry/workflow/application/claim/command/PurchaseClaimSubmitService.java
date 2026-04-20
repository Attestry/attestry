package io.attestry.workflow.application.claim.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.claim.result.SubmitPurchaseClaimResult;
import io.attestry.workflow.application.claim.internal.PurchaseClaimEvidenceViewResolver;
import io.attestry.workflow.application.claim.internal.PurchaseClaimLookupService;
import io.attestry.workflow.application.claim.internal.PurchaseClaimViewFactory;
import io.attestry.workflow.application.claim.view.ClaimEvidenceView;
import io.attestry.workflow.application.claim.view.MyClaimView;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.command.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.shipment.command.EvidenceCompleteResult;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseClaimSubmitService implements PurchaseClaimSubmitUseCase {

    private final PurchaseClaimRepository purchaseClaimRepository;
    private final WorkflowEvidencePort evidencePort;
    private final PurchaseClaimEvidenceService evidenceService;
    private final PurchaseClaimLookupService claimLookupService;
    private final PurchaseClaimEvidenceViewResolver evidenceViewResolver;
    private final PurchaseClaimViewFactory claimViewFactory;
    private final Clock clock;


    @Override
    @Transactional
    public SubmitPurchaseClaimResult submit(WorkflowActorContext principal, SubmitPurchaseClaimCommand command) {
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
        return claimViewFactory.toSubmitResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyClaimView> listMyClaims(WorkflowActorContext principal) {
        return purchaseClaimRepository.findByClaimantUserId(principal.userId()).stream()
            .map(claim -> claimViewFactory.toMyClaimView(
                claim,
                evidenceViewResolver.resolveReadyEvidenceViews(claim.evidenceGroupId())
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimEvidenceView> listMyClaimEvidences(WorkflowActorContext principal, String claimId) {
        PurchaseClaim claim = claimLookupService.getOwnedByClaimant(claimId, principal.userId());
        return evidenceViewResolver.resolveReadyEvidenceViews(claim.evidenceGroupId());
    }

    @Override
    @Transactional
    public PresignedEvidenceUploadResult presignEvidence(WorkflowActorContext principal, PresignClaimEvidenceCommand command) {
        return evidenceService.presignEvidence(principal, command);
    }

    @Override
    @Transactional
    public EvidenceCompleteResult completeEvidence(WorkflowActorContext principal, CompleteClaimEvidenceCommand command) {
        return evidenceService.completeEvidence(principal, command);
    }

    private void assertEvidenceGroupOwnedByClaimant(String evidenceGroupId, String claimantUserId) {
        if (evidenceGroupId == null || evidenceGroupId.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "evidenceGroupId is required");
        }
        WorkflowEvidencePort.EvidenceGroupScopeRecord scope = evidencePort.findEvidenceGroupScope(evidenceGroupId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence group not found"));
        if (scope.ownerUserId() == null || !scope.ownerUserId().equals(claimantUserId)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.TENANT_ISOLATION_VIOLATION,
                "Evidence group is not owned by claimant"
            );
        }
    }
}
