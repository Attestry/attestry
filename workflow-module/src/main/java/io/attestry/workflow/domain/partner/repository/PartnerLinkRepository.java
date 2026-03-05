package io.attestry.workflow.domain.partner.repository;

import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import java.util.List;
import java.util.Optional;

public interface PartnerLinkRepository {
    PartnerLink save(PartnerLink partnerLink);

    Optional<PartnerLink> findById(String partnerLinkId);

    List<PartnerLink> findByTenantId(String tenantId);
    List<PartnerLink> findByTenantIdAndStatus(String tenantId, PartnerLinkStatus status);

    boolean existsBySourceAndTargetAndTypeAndStatus(
        String sourceTenantId,
        String targetTenantId,
        PartnerType partnerType,
        PartnerLinkStatus status
    );
}
