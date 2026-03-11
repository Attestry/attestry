package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort.PagedReleaseCandidateResult;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort.PagedShipmentReadResult;

public interface ShipmentProductReadCustomRepository {

    PagedReleaseCandidateResult findReleaseCandidatesWithFilters(
        String tenantId, int page, int size, String keyword
    );

    PagedShipmentReadResult findShipmentsWithFilters(
        String tenantId, int page, int size, String keyword
    );
}
