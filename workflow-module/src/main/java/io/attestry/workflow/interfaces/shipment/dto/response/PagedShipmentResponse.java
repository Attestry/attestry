package io.attestry.workflow.interfaces.shipment.dto.response;

import java.util.List;

public record PagedShipmentResponse(
        List<ShipmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
