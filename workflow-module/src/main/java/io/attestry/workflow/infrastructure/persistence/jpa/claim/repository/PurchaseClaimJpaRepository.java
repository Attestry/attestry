package io.attestry.workflow.infrastructure.persistence.jpa.claim.repository;

import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import io.attestry.workflow.infrastructure.persistence.jpa.claim.entity.WorkflowPurchaseClaimJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseClaimJpaRepository extends JpaRepository<WorkflowPurchaseClaimJpaEntity, String> {

    List<WorkflowPurchaseClaimJpaEntity> findByClaimantUserIdOrderBySubmittedAtDesc(String claimantUserId);

    List<WorkflowPurchaseClaimJpaEntity> findByStatusOrderBySubmittedAtAsc(PurchaseClaimStatus status);
}
