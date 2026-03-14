package io.attestry.workflow.domain.claim.repository;

import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import java.util.List;
import java.util.Optional;

public interface PurchaseClaimRepository {

    PurchaseClaim save(PurchaseClaim claim);

    Optional<PurchaseClaim> findById(String claimId);

    List<PurchaseClaim> findByClaimantUserId(String userId);

    List<PurchaseClaim> findByStatus(PurchaseClaimStatus status);
}
