package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.PartnerLinkJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.PartnerLinkJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPartnerLinkRepositoryAdapter implements PartnerLinkRepository {

    private final PartnerLinkJpaRepository repository;

    public JpaPartnerLinkRepositoryAdapter(PartnerLinkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PartnerLink save(PartnerLink partnerLink) {
        long rowVersion = repository.findById(partnerLink.partnerLinkId())
            .map(PartnerLinkJpaEntity::getRowVersion)
            .orElse(0L);
        return toDomain(repository.save(toEntity(partnerLink, rowVersion)));
    }

    @Override
    public Optional<PartnerLink> findById(String partnerLinkId) {
        return repository.findById(partnerLinkId).map(this::toDomain);
    }

    @Override
    public List<PartnerLink> findByTenantId(String tenantId) {
        return repository.findBySourceTenantIdOrTargetTenantId(tenantId, tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBySourceAndTargetAndTypeAndStatus(
        String sourceTenantId,
        String targetTenantId,
        PartnerType partnerType,
        PartnerLinkStatus status
    ) {
        return repository.existsBySourceTenantIdAndTargetTenantIdAndPartnerTypeAndStatus(
            sourceTenantId,
            targetTenantId,
            partnerType,
            status
        );
    }

    private PartnerLink toDomain(PartnerLinkJpaEntity entity) {
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
            entity.getTerminatedAt(),
            entity.getReason()
        );
    }

    private PartnerLinkJpaEntity toEntity(PartnerLink link, long rowVersion) {
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
            link.terminatedAt(),
            link.reason(),
            rowVersion
        );
    }
}
