package io.attestry.workflow.application.servicerequest.query;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.servicerequest.view.PagedServiceRequestView;

public interface ServiceRequestQueryUseCase {

    PagedServiceRequestView listMyRequests(WorkflowActorContext principal, String status, int page, int size);

    PagedServiceRequestView listProviderRequests(WorkflowActorContext principal, String tenantId, String status, int page, int size);
}
