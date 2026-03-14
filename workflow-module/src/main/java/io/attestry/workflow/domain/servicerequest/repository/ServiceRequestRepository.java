package io.attestry.workflow.domain.servicerequest.repository;

import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository {

    ServiceRequest save(ServiceRequest request);

    Optional<ServiceRequest> findById(String serviceRequestId);

    boolean existsOpenByPassportId(String passportId);

    List<ServiceRequest> findByOwnerUserId(String ownerUserId, ServiceRequestStatus status, int page, int size);

    long countByOwnerUserId(String ownerUserId, ServiceRequestStatus status);

    List<ServiceRequest> findByProviderTenantId(String providerTenantId, ServiceRequestStatus status, int page, int size);

    long countByProviderTenantId(String providerTenantId, ServiceRequestStatus status);
}
