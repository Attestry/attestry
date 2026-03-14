package io.attestry.workflow.domain.distribution.repository;

import io.attestry.workflow.domain.distribution.model.Distribution;
import java.util.List;
import java.util.Optional;

public interface DistributionRepository {

    Distribution save(Distribution distribution);

    Optional<Distribution> findById(String distributionId);

    List<Distribution> findBySourceTenantId(String sourceTenantId);
}
