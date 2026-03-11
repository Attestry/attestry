package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.partner.model.PartnerLink;
import io.attestry.workflow.domain.partner.repository.PartnerLinkRepository;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import io.attestry.workflow.domain.partner.model.PartnerType;
import io.attestry.workflow.infrastructure.persistence.jpa.entity.PartnerLinkJpaEntity;
import io.attestry.workflow.infrastructure.persistence.jpa.mapper.PartnerLinkMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.PartnerLinkJpaRepository;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaPartnerLinkRepositoryAdapter implements PartnerLinkRepository {

    private final PartnerLinkJpaRepository repository;
    private final PartnerLinkMapper mapper;


    @Override
    public PartnerLink save(PartnerLink partnerLink) {
        try {
            long rowVersion = repository.findById(partnerLink.partnerLinkId())
                .map(PartnerLinkJpaEntity::getRowVersion)
                .orElse(0L);
            return mapper.toDomain(repository.save(mapper.toEntity(partnerLink, rowVersion)));
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
        return repository.findById(partnerLinkId).map(mapper::toDomain);
    }

    @Override
    public List<PartnerLink> findByTenantId(String tenantId, PartnerLinkStatus status) {
        if (status == null) {
            return repository.findBySourceTenantIdOrTargetTenantId(tenantId, tenantId)
                .stream().map(mapper::toDomain).toList();
        }
        return repository.findBySourceTenantIdAndStatusOrTargetTenantIdAndStatus(
            tenantId, status, tenantId, status
        ).stream().map(mapper::toDomain).toList();
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

    private boolean isDuplicateStatusConstraint(DataIntegrityViolationException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        String message = root != null ? root.getMessage() : ex.getMessage();
        return message != null && message.contains("uq_partner_links_by_status");
    }
}
