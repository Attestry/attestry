package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.internal.ServiceRequestAccessPolicy;
import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;
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
public class ServiceRejectService implements ServiceRejectUseCase {

    private final ServiceRequestAccessPolicy accessPolicy;
    private final ServiceRequestLookupService serviceRequestLookupService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestResultFactory resultFactory;
    private final Clock clock;


    @Override
    @Transactional
    public RejectServiceRequestResult reject(
        WorkflowActorContext principal,
        String tenantId,
        String serviceRequestId,
        RejectServiceRequestCommand command
    ) {
        accessPolicy.assertProviderCompletePermission(principal, tenantId, "service:reject:" + serviceRequestId);

        ServiceRequest request = serviceRequestLookupService.getPendingById(serviceRequestId);
        accessPolicy.assertProviderRequestAccess(tenantId, request, "reject");

        Instant now = Instant.now(clock);
        ServiceRequest rejected = request.reject(command.reason(), now);
        ServiceRequest saved = serviceRequestRepository.save(rejected);
        return resultFactory.toRejectResult(saved, now);
    }
}
