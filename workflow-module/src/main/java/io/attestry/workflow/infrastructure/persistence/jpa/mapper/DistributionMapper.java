package io.attestry.workflow.infrastructure.persistence.jpa.mapper;

import io.attestry.workflow.domain.distribution.model.Distribution;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.DistributionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DistributionMapper {

    public Distribution toDomain(DistributionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
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

    public DistributionJpaEntity toEntity(Distribution distribution, long rowVersion) {
        if (distribution == null) {
            return null;
        }
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
