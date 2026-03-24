package io.attestry.workflow.application.distribution.view;

import java.util.List;

public record PagedDistributionView(
    List<DistributionView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
