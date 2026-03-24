package io.attestry.workflow.application.distribution.view;

import java.util.List;

public record PagedDistributionCandidateView(
    List<DistributionCandidateView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
