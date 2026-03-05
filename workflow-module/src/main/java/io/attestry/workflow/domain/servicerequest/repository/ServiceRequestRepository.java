package io.attestry.workflow.domain.servicerequest.repository;

import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import java.util.Optional;

public interface ServiceRequestRepository {

    ServiceRequest save(ServiceRequest request);

    Optional<ServiceRequest> findById(String serviceRequestId);

    boolean existsSubmittedByPassportId(String passportId);
}
