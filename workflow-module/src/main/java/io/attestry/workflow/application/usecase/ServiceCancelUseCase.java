package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;

public interface ServiceCancelUseCase {

    CancelServiceRequestResult cancel(AuthPrincipal principal, String serviceRequestId, String cancelReason);
}
