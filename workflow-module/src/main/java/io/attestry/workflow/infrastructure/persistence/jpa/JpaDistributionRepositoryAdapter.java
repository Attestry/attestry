package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.domain.distribution.repository.DistributionRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.DistributionJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.mapper.DistributionMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.DistributionJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDistributionRepositoryAdapter implements DistributionRepository {

    private final DistributionJpaRepository repository;
    private final DistributionMapper mapper;

    public JpaDistributionRepositoryAdapter(DistributionJpaRepository repository, DistributionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Distribution save(Distribution distribution) {
        long rowVersion = repository.findById(distribution.distributionId())
            .map(DistributionJpaEntity::getRowVersion)
            .orElse(0L);
        return mapper.toDomain(repository.save(mapper.toEntity(distribution, rowVersion)));
    }

    @Override
    public Optional<Distribution> findById(String distributionId) {
        return repository.findById(distributionId).map(mapper::toDomain);
    }

    @Override
    public List<Distribution> findBySourceTenantId(String sourceTenantId) {
        return repository.findBySourceTenantId(sourceTenantId).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
