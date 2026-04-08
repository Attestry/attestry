package io.attestry.workflow.application.claim.internal;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseClaimLookupService {

    private final PurchaseClaimRepository purchaseClaimRepository;

    public PurchaseClaim getOwnedByClaimant(String claimId, String claimantUserId) {
        PurchaseClaim claim = getById(claimId);
        if (!claim.claimantUserId().equals(claimantUserId)) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.FORBIDDEN_SCOPE,
                "Only claimant can access evidences"
            );
        }
        return claim;
    }

    public PurchaseClaim getSubmitted(String claimId) {
        PurchaseClaim claim = getById(claimId);
        if (claim.status() != PurchaseClaimStatus.SUBMITTED) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.CLAIM_INVALID_STATE,
                "Claim is not in SUBMITTED state"
            );
        }
        return claim;
    }

    private PurchaseClaim getById(String claimId) {
        return purchaseClaimRepository.findById(claimId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.CLAIM_NOT_FOUND,
                "Purchase claim not found"
            ));
    }
}
