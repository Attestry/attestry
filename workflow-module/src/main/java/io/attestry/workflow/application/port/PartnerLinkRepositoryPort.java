package io.attestry.workflow.application.port;

import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import java.util.List;
import java.util.Optional;

public interface PartnerLinkRepositoryPort {
    PartnerLink save(PartnerLink partnerLink);

    Optional<PartnerLink> findById(String partnerLinkId);

    List<PartnerLink> findByTenantId(String tenantId);

    boolean existsBySourceAndTargetAndTypeAndStatus(
        String sourceTenantId,
        String targetTenantId,
        PartnerType partnerType,
        PartnerLinkStatus status
    );
}
