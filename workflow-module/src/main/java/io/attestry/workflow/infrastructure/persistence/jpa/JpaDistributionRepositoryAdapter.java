package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.distribution.repository.DistributionRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.DistributionJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.DistributionJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDistributionRepositoryAdapter implements DistributionRepository {

    private final DistributionJpaRepository repository;

    public JpaDistributionRepositoryAdapter(DistributionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Distribution save(Distribution distribution) {
        long rowVersion = repository.findById(distribution.distributionId())
            .map(DistributionJpaEntity::getRowVersion)
            .orElse(0L);
        return toDomain(repository.save(toEntity(distribution, rowVersion)));
    }

    @Override
    public Optional<Distribution> findById(String distributionId) {
        return repository.findById(distributionId).map(this::toDomain);
    }

    @Override
    public List<Distribution> findBySourceTenantId(String sourceTenantId) {
        return repository.findBySourceTenantId(sourceTenantId).stream()
            .map(this::toDomain)
            .toList();
    }

    private Distribution toDomain(DistributionJpaEntity entity) {
        return new Distribution(
            entity.getDistributionId(),
            entity.getPassportId(),
            entity.getSourceTenantId(),
            entity.getTargetTenantId(),
            entity.getPartnerLinkId(),
            entity.getDelegationId(),
            entity.getStatus(),
            entity.getDistributedByUserId(),
            entity.getDistributedAt(),
            entity.getRecalledByUserId(),
            entity.getRecalledAt(),
            entity.getRecallReason()
        );
    }

    private DistributionJpaEntity toEntity(Distribution distribution, long rowVersion) {
        return new DistributionJpaEntity(
            distribution.distributionId(),
            distribution.passportId(),
            distribution.sourceTenantId(),
            distribution.targetTenantId(),
            distribution.partnerLinkId(),
            distribution.delegationId(),
            distribution.status(),
            distribution.distributedByUserId(),
            distribution.distributedAt(),
            distribution.recalledByUserId(),
            distribution.recalledAt(),
            distribution.recallReason(),
            rowVersion
        );
    }
}
