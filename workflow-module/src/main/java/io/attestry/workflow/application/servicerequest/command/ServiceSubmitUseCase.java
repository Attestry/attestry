package io.attestry.workflow.application.servicerequest.command;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;

public interface ServiceSubmitUseCase {

    SubmitServiceRequestResult submit(WorkflowActorContext principal, SubmitServiceRequestCommand command);
}
