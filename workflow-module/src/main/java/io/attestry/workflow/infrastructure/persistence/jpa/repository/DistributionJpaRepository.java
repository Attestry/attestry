package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.infrastructure.persistence.jpa.entity.DistributionJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributionJpaRepository extends JpaRepository<DistributionJpaEntity, String> {

    List<DistributionJpaEntity> findBySourceTenantId(String sourceTenantId);
}
