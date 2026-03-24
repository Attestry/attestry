package io.attestry.workflow.interfaces.servicerequest.dto.response;

import io.attestry.workflow.application.servicerequest.view.PagedServiceRequestView;
import java.util.List;

public record PagedServiceRequestResponse(
    List<ServiceRequestListItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static PagedServiceRequestResponse from(PagedServiceRequestView result) {
        return new PagedServiceRequestResponse(
            result.content().stream().map(ServiceRequestListItemResponse::from).toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }
}
