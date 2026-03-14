package io.attestry.product.application.port.projection;

import java.time.Instant;

public interface ProductShipmentProjectionWritePort {

    void refreshShipmentProjection(String passportId, String sourceEventId, Long sourceEventVersion, Instant updatedAt);
}
