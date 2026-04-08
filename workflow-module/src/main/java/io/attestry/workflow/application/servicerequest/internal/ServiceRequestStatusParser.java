package io.attestry.workflow.application.servicerequest.internal;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequestStatus;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestStatusParser {

    public ServiceRequestStatus parse(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ServiceRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Invalid service request status filter"
            );
        }
    }
}
