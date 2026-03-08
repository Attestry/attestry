package io.attestry.workflow.domain.claim.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Instant;

public record PurchaseClaim(
    String claimId,
    String claimantUserId,
    String serialNumber,
    String modelName,
    String evidenceGroupId,
    String note,
    PurchaseClaimStatus status,
    Instant submittedAt,
    String reviewedByUserId,
    Instant reviewedAt,
    String rejectionReason,
    String passportId,
    String assetId
) {

    public static PurchaseClaim submit(
        String claimId,
        String claimantUserId,
        String serialNumber,
        String modelName,
        String evidenceGroupId,
        String note,
        Instant now
    ) {
        if (claimId == null || claimId.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "claimId is required");
        }
        if (claimantUserId == null || claimantUserId.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "claimantUserId is required");
        }
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "serialNumber is required");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "modelName is required");
        }
        if (evidenceGroupId == null || evidenceGroupId.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "evidenceGroupId is required");
        }

        return new PurchaseClaim(
            claimId, claimantUserId,
            serialNumber, modelName, evidenceGroupId, note,
            PurchaseClaimStatus.SUBMITTED, now,
            null, null, null,
            null, null
        );
    }

    public PurchaseClaim approve(String reviewerUserId, String passportId, String assetId, Instant now) {
        if (status != PurchaseClaimStatus.SUBMITTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.CLAIM_INVALID_STATE, "Only SUBMITTED claims can be approved");
        }
        return new PurchaseClaim(
            claimId, claimantUserId,
            serialNumber, modelName, evidenceGroupId, note,
            PurchaseClaimStatus.APPROVED, submittedAt,
            reviewerUserId, now, null,
            passportId, assetId
        );
    }

    public PurchaseClaim reject(String reviewerUserId, String reason, Instant now) {
        if (status != PurchaseClaimStatus.SUBMITTED) {
            throw new WorkflowDomainException(WorkflowErrorCode.CLAIM_INVALID_STATE, "Only SUBMITTED claims can be rejected");
        }
        return new PurchaseClaim(
            claimId, claimantUserId,
            serialNumber, modelName, evidenceGroupId, note,
            PurchaseClaimStatus.REJECTED, submittedAt,
            reviewerUserId, now, reason,
            null, null
        );
    }
}
