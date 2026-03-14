package io.attestry.product.application.port.projection;

import java.time.Instant;

public interface ProductDistributionProjectionWritePort {

    void refreshDistributionProjection(String passportId, String sourceEventId, Long sourceEventVersion, Instant updatedAt);
}
