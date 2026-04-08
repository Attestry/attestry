package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.command.RejectServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;

public interface ServiceRejectUseCase {

    RejectServiceRequestResult reject(
        WorkflowActorContext principal,
        String tenantId,
        String serviceRequestId,
        RejectServiceRequestCommand command
    );
}
