package io.attestry.workflow.application.shipment.view;

import java.util.List;

public record PagedShipmentView(
    List<ShipmentView> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
