package io.attestry.workflow.infrastructure.persistence.jpa.claim;

import io.attestry.workflow.domain.claim.model.PurchaseClaim;
import io.attestry.workflow.domain.claim.model.PurchaseClaimStatus;
import io.attestry.workflow.domain.claim.repository.PurchaseClaimRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.claim.mapper.PurchaseClaimMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.claim.repository.PurchaseClaimJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaPurchaseClaimRepositoryAdapter implements PurchaseClaimRepository {

    private final PurchaseClaimJpaRepository repository;
    private final PurchaseClaimMapper mapper;

    @Override
    public PurchaseClaim save(PurchaseClaim claim) {
        return mapper.toDomain(repository.save(mapper.toEntity(claim)));
    }

    @Override
    public Optional<PurchaseClaim> findById(String claimId) {
        return repository.findById(claimId).map(mapper::toDomain);
    }

    @Override
    public List<PurchaseClaim> findByClaimantUserId(String userId) {
        return repository.findByClaimantUserIdOrderBySubmittedAtDesc(userId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<PurchaseClaim> findByStatus(PurchaseClaimStatus status) {
        return repository.findByStatusOrderBySubmittedAtAsc(status)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
