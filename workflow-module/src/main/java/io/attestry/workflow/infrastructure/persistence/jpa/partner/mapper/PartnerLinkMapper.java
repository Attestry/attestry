package io.attestry.workflow.infrastructure.persistence.jpa.partner.mapper;

import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.infrastructure.persistence.jpa.partner.entity.PartnerLinkJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PartnerLinkMapper {

    public PartnerLink toDomain(PartnerLinkJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PartnerLink(
            entity.getPartnerLinkId(),
            entity.getSourceTenantId(),
            entity.getTargetTenantId(),
            entity.getPartnerType(),
            entity.getStatus(),
            entity.getCreatedByUserId(),
            entity.getCreatedAt(),
            entity.getApprovedByUserId(),
            entity.getApprovedAt(),
            entity.getExpiresAt(),
            entity.getTerminatedAt(),
            entity.getReason()
        );
    }

    public PartnerLinkJpaEntity toEntity(PartnerLink link, long rowVersion) {
        if (link == null) {
            return null;
        }
        return new PartnerLinkJpaEntity(
            link.partnerLinkId(),
            link.sourceTenantId(),
            link.targetTenantId(),
            link.partnerType(),
            link.status(),
            link.createdByUserId(),
            link.createdAt(),
            link.approvedByUserId(),
            link.approvedAt(),
            link.expiresAt(),
            link.terminatedAt(),
            link.reason(),
            rowVersion
        );
    }
}
