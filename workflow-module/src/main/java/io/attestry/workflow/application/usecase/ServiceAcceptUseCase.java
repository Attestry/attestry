package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.AcceptServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.AcceptServiceRequestResult;

public interface ServiceAcceptUseCase {

    AcceptServiceRequestResult accept(
        AuthPrincipal principal,
        String tenantId,
        String serviceRequestId,
        AcceptServiceRequestCommand command
    );
}
