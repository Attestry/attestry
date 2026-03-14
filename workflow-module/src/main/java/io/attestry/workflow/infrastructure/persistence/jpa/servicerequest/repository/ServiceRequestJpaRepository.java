package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.repository;

import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.entity.WorkflowServiceRequestJpaEntity;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestJpaRepository extends JpaRepository<WorkflowServiceRequestJpaEntity, String> {

    boolean existsByPassportIdAndStatusIn(String passportId, Collection<ServiceRequestStatus> statuses);

    Page<WorkflowServiceRequestJpaEntity> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId, Pageable pageable);

    Page<WorkflowServiceRequestJpaEntity> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(
        String ownerUserId,
        ServiceRequestStatus status,
        Pageable pageable
    );

    long countByOwnerUserId(String ownerUserId);

    long countByOwnerUserIdAndStatus(String ownerUserId, ServiceRequestStatus status);

    Page<WorkflowServiceRequestJpaEntity> findByProviderTenantIdOrderByCreatedAtDesc(String providerTenantId, Pageable pageable);

    Page<WorkflowServiceRequestJpaEntity> findByProviderTenantIdAndStatusOrderByCreatedAtDesc(
        String providerTenantId,
        ServiceRequestStatus status,
        Pageable pageable
    );

    long countByProviderTenantId(String providerTenantId);

    long countByProviderTenantIdAndStatus(String providerTenantId, ServiceRequestStatus status);
}
