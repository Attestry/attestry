package io.attestry.workflow.domain.shipment.repository;

import io.attestry.workflow.domain.shipment.model.Shipment;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    boolean existsActiveReleasedByPassportId(String passportId);

    int nextShipmentRound(String passportId);

    Shipment saveRelease(Shipment shipment);

    Shipment saveReturn(Shipment shipment);

    Optional<Shipment> findByShipmentId(String shipmentId);

    List<Shipment> findByPassportId(String passportId);
}
