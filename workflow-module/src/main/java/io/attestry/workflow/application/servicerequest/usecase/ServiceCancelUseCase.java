package io.attestry.workflow.application.servicerequest.usecase;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.result.CancelServiceRequestResult;

public interface ServiceCancelUseCase {

    CancelServiceRequestResult cancel(WorkflowActorContext principal, String serviceRequestId, String cancelReason);
}
