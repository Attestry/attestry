package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.command.RejectServiceRequestCommand;
import io.attestry.workflow.application.servicerequest.result.RejectServiceRequestResult;

public interface ServiceRejectUseCase {

    RejectServiceRequestResult reject(
        AuthPrincipal principal,
        String tenantId,
        String serviceRequestId,
        RejectServiceRequestCommand command
    );
}
