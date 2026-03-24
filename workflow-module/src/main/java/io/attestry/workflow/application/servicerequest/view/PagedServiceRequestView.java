package io.attestry.workflow.application.servicerequest.view;

import java.util.List;

public record PagedServiceRequestView(
    List<ServiceRequestListItemView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
