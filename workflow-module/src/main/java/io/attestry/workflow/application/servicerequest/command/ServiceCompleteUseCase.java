package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;

public interface ServiceCompleteUseCase {

    CompleteServiceRequestResult complete(
        WorkflowActorContext principal,
        String tenantId,
        String serviceRequestId,
        CompleteServiceRequestCommand command
    );
}
