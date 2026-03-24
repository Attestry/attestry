package io.attestry.workflow.application.manual.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.manual.command.SendPassportManualCommand;
import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;
import io.attestry.workflow.application.manual.result.SendPassportManualResult;

public interface PassportManualUseCase {

    PassportManualRecipientResult getRecipient(WorkflowActorContext principal, String tenantId, String passportId);

    SendPassportManualResult send(WorkflowActorContext principal, String tenantId, SendPassportManualCommand command);
}
