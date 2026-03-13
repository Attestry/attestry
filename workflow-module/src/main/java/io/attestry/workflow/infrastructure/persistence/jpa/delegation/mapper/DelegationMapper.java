package io.attestry.workflow.infrastructure.persistence.jpa.delegation.mapper;

import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.infrastructure.persistence.jpa.delegation.entity.DelegationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DelegationMapper {

    public Delegation toDomain(DelegationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Delegation(
            entity.getDelegationId(),
            entity.getPartnerLinkId(),
            entity.getSourceTenantId(),
            entity.getTargetTenantId(),
            entity.getResourceType(),
            entity.getResourceId(),
            entity.getPermissionCode(),
            entity.getStatus(),
            entity.getExpiresAt(),
            entity.getGrantedByUserId(),
            entity.getCreatedAt(),
            entity.getRevokedByUserId(),
            entity.getRevokedAt(),
            entity.getReason()
        );
    }

    public DelegationJpaEntity toEntity(Delegation delegation, long rowVersion) {
        if (delegation == null) {
            return null;
        }
        return new DelegationJpaEntity(
            delegation.delegationId(),
            delegation.partnerLinkId(),
            delegation.sourceTenantId(),
            delegation.targetTenantId(),
            delegation.resourceType(),
            delegation.resourceId(),
            delegation.permissionCode(),
            delegation.status(),
            delegation.expiresAt(),
            delegation.grantedByUserId(),
            delegation.createdAt(),
            delegation.revokedByUserId(),
            delegation.revokedAt(),
            delegation.reason(),
            rowVersion
        );
    }
}
