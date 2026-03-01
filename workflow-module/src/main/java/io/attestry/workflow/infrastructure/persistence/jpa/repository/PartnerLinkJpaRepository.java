package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.PartnerLinkJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerLinkJpaRepository extends JpaRepository<PartnerLinkJpaEntity, String> {
    List<PartnerLinkJpaEntity> findByBrandTenantIdOrPartnerTenantId(String brandTenantId, String partnerTenantId);

    boolean existsByBrandTenantIdAndPartnerTenantIdAndPartnerTypeAndStatus(
        String brandTenantId,
        String partnerTenantId,
        PartnerType partnerType,
        PartnerLinkStatus status
    );
}
