package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.AcceptServiceRequestResult;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestResultFactory;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestLookupService;
import io.attestry.workflow.domain.servicerequest.model.ServiceRequest;
import io.attestry.workflow.domain.servicerequest.repository.ServiceRequestRepository;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceAcceptService implements ServiceAcceptUseCase {

    private final ServiceRequestAccessPolicy accessPolicy;
    private final ServiceRequestLookupService serviceRequestLookupService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestResultFactory resultFactory;
    private final Clock clock;

    @Override
    @Transactional
    public AcceptServiceRequestResult accept(
        WorkflowActorContext principal,
        String tenantId,
        String serviceRequestId,
        AcceptServiceRequestCommand command
    ) {
        accessPolicy.assertProviderCompletePermission(principal, tenantId, "service:accept:" + serviceRequestId);

        ServiceRequest request = serviceRequestLookupService.getPendingById(serviceRequestId);
        accessPolicy.assertProviderRequestAccess(tenantId, request, "accept");
        Instant now = Instant.now(clock);
        ServiceRequest accepted = request.accept(command.serviceType(), command.description(), now);
        ServiceRequest saved = serviceRequestRepository.save(accepted);
        return resultFactory.toAcceptResult(saved, now);
    }
}
