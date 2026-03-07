package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.PartnerLinkJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.PartnerLinkJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPartnerLinkRepositoryAdapter implements PartnerLinkRepository {

    private final PartnerLinkJpaRepository repository;

    public JpaPartnerLinkRepositoryAdapter(PartnerLinkJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PartnerLink save(PartnerLink partnerLink) {
        try {
            long rowVersion = repository.findById(partnerLink.partnerLinkId())
                .map(PartnerLinkJpaEntity::getRowVersion)
                .orElse(0L);
            return toDomain(repository.save(toEntity(partnerLink, rowVersion)));
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateStatusConstraint(ex)) {
                throw new WorkflowDomainException(
                    WorkflowErrorCode.PARTNER_LINK_DUPLICATE_STATUS,
                    "Duplicate partner link status for the same source/target/type"
                );
            }
            throw ex;
        }
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
    public List<PartnerLink> findByTenantIdAndStatus(String tenantId, PartnerLinkStatus status) {
        return repository.findBySourceTenantIdAndStatusOrTargetTenantIdAndStatus(
            tenantId,
            status,
            tenantId,
            status
        ).stream().map(this::toDomain).toList();
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
            entity.getExpiresAt(),
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
            link.expiresAt(),
            link.terminatedAt(),
            link.reason(),
            rowVersion
        );
    }

    private boolean isDuplicateStatusConstraint(DataIntegrityViolationException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        String message = root != null ? root.getMessage() : ex.getMessage();
        return message != null && message.contains("uq_partner_links_by_status");
    }
}
