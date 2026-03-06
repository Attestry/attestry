package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.CompleteServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.CompleteServiceRequestResult;

public interface ServiceCompleteUseCase {

    CompleteServiceRequestResult complete(
        AuthPrincipal principal,
        String tenantId,
        String serviceRequestId,
        CompleteServiceRequestCommand command
    );
}
