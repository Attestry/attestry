package io.attestry.workflow.application.servicerequest.internal;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceRequestLookupService {

    private final ServiceRequestRepository serviceRequestRepository;

    public ServiceRequest getByIdOrThrow(String serviceRequestId) {
        return serviceRequestRepository.findById(serviceRequestId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.SERVICE_REQUEST_NOT_FOUND,
                "Service request not found"
            ));
    }

    public ServiceRequest getPendingById(String serviceRequestId) {
        ServiceRequest request = getByIdOrThrow(serviceRequestId);
        if (request.status() != ServiceRequestStatus.PENDING) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.SERVICE_REQUEST_INVALID_STATE,
                "Only PENDING service request can be processed"
            );
        }
        return request;
    }
}
