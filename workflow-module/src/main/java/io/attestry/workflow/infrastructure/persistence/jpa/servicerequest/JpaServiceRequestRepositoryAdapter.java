package io.attestry.workflow.infrastructure.persistence.jpa.servicerequest;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.mapper.ServiceRequestMapper;
import io.attestry.workflow.infrastructure.persistence.jpa.servicerequest.repository.ServiceRequestJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaServiceRequestRepositoryAdapter implements ServiceRequestRepository {

    private static final String OPEN_REQUEST_CONSTRAINT = "uq_workflow_service_requests_open_passport";
    private static final List<ServiceRequestStatus> OPEN_STATUSES = List.of(
        ServiceRequestStatus.PENDING,
        ServiceRequestStatus.ACCEPTED
    );

    private final ServiceRequestJpaRepository repository;
    private final ServiceRequestMapper mapper;

    @Override
    public ServiceRequest save(ServiceRequest request) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(request)));
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateOpenRequestConstraint(ex)) {
                throw new WorkflowDomainException(
                    WorkflowErrorCode.SERVICE_REQUEST_ALREADY_SUBMITTED,
                    "An open service request already exists for this passport"
                );
            }
            throw ex;
        }
    }

    @Override
    public Optional<ServiceRequest> findById(String serviceRequestId) {
        return repository.findById(serviceRequestId).map(mapper::toDomain);
    }

    @Override
    public boolean existsOpenByPassportId(String passportId) {
        return repository.existsByPassportIdAndStatusIn(passportId, OPEN_STATUSES);
    }

    @Override
    public List<ServiceRequest> findByOwnerUserId(String ownerUserId, ServiceRequestStatus status, int page, int size) {
        PageRequest pageable = toPageRequest(page, size);
        if (status == null) {
            return repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId, pageable)
                .stream().map(mapper::toDomain).toList();
        }
        return repository.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(ownerUserId, status, pageable)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByOwnerUserId(String ownerUserId, ServiceRequestStatus status) {
        return status == null
            ? repository.countByOwnerUserId(ownerUserId)
            : repository.countByOwnerUserIdAndStatus(ownerUserId, status);
    }

    @Override
    public List<ServiceRequest> findByProviderTenantId(String providerTenantId, ServiceRequestStatus status, int page, int size) {
        PageRequest pageable = toPageRequest(page, size);
        if (status == null) {
            return repository.findByProviderTenantIdOrderByCreatedAtDesc(providerTenantId, pageable)
                .stream().map(mapper::toDomain).toList();
        }
        return repository.findByProviderTenantIdAndStatusOrderByCreatedAtDesc(providerTenantId, status, pageable)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByProviderTenantId(String providerTenantId, ServiceRequestStatus status) {
        return status == null
            ? repository.countByProviderTenantId(providerTenantId)
            : repository.countByProviderTenantIdAndStatus(providerTenantId, status);
    }

    private PageRequest toPageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        return PageRequest.of(safePage, safeSize);
    }

    private boolean isDuplicateOpenRequestConstraint(DataIntegrityViolationException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        String message = root != null ? root.getMessage() : ex.getMessage();
        return message != null && message.contains(OPEN_REQUEST_CONSTRAINT);
    }
}
