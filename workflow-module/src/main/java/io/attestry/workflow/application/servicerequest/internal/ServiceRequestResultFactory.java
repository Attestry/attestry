package io.attestry.workflow.application.servicerequest.internal;

import io.attestry.workflow.application.servicerequest.result.AcceptServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;
import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestResultFactory {

    public AcceptServiceRequestResult toAcceptResult(ServiceRequest request, Instant acceptedAt) {
        return new AcceptServiceRequestResult(
            request.serviceRequestId(),
            request.passportId(),
            request.status().name(),
            acceptedAt
        );
    }

    public CancelServiceRequestResult toCancelResult(ServiceRequest request) {
        return new CancelServiceRequestResult(
            request.serviceRequestId(),
            request.passportId(),
            request.status().name(),
            request.cancelledAt()
        );
    }

    public RejectServiceRequestResult toRejectResult(ServiceRequest request, Instant rejectedAt) {
        return new RejectServiceRequestResult(
            request.serviceRequestId(),
            request.passportId(),
            request.status().name(),
            rejectedAt
        );
    }
}
