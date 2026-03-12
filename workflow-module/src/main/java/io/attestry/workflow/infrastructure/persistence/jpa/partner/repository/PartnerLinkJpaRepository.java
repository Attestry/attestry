package io.attestry.workflow.infrastructure.persistence.jpa.partner.repository;

import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.infrastructure.persistence.jpa.partner.entity.PartnerLinkJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerLinkJpaRepository extends JpaRepository<PartnerLinkJpaEntity, String> {
    List<PartnerLinkJpaEntity> findBySourceTenantIdOrTargetTenantId(String sourceTenantId, String targetTenantId);
    List<PartnerLinkJpaEntity> findBySourceTenantIdAndStatusOrTargetTenantIdAndStatus(
        String sourceTenantId,
        PartnerLinkStatus sourceStatus,
        String targetTenantId,
        PartnerLinkStatus targetStatus
    );

    boolean existsBySourceTenantIdAndTargetTenantIdAndPartnerTypeAndStatus(
        String sourceTenantId,
        String targetTenantId,
        PartnerType partnerType,
        PartnerLinkStatus status
    );
}
