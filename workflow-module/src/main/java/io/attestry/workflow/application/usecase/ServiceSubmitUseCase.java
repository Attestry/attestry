package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.SubmitServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.SubmitServiceRequestResult;

public interface ServiceSubmitUseCase {

    SubmitServiceRequestResult submit(AuthPrincipal principal, String tenantId, String groupId, SubmitServiceRequestCommand command);
}
