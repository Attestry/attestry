package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.command.AcceptServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.AcceptServiceRequestResult;

public interface ServiceAcceptUseCase {

    AcceptServiceRequestResult accept(
        WorkflowActorContext principal,
        String tenantId,
        String serviceRequestId,
        AcceptServiceRequestCommand command
    );
}
