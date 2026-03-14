package io.attestry.workflow.infrastructure.persistence.jpa.claim.mapper;

import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.infrastructure.persistence.jpa.claim.entity.WorkflowPurchaseClaimJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PurchaseClaimMapper {

    public PurchaseClaim toDomain(WorkflowPurchaseClaimJpaEntity entity) {
        return new PurchaseClaim(
            entity.getClaimId(),
            entity.getClaimantUserId(),
            entity.getSerialNumber(),
            entity.getModelName(),
            entity.getEvidenceGroupId(),
            entity.getNote(),
            entity.getStatus(),
            entity.getSubmittedAt(),
            entity.getReviewedByUserId(),
            entity.getReviewedAt(),
            entity.getRejectionReason(),
            entity.getPassportId(),
            entity.getAssetId()
        );
    }

    public WorkflowPurchaseClaimJpaEntity toEntity(PurchaseClaim domain) {
        return new WorkflowPurchaseClaimJpaEntity(
            domain.claimId(),
            domain.claimantUserId(),
            domain.serialNumber(),
            domain.modelName(),
            domain.evidenceGroupId(),
            domain.note(),
            domain.status(),
            domain.submittedAt(),
            domain.reviewedByUserId(),
            domain.reviewedAt(),
            domain.rejectionReason(),
            domain.passportId(),
            domain.assetId()
        );
    }
}
