package io.attestry.workflow.application.shipment.query;

import java.util.List;

public record PagedReleaseCandidateView(
    List<ShipmentReleaseCandidateView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
