package io.attestry.product.application.port.projection;

import java.time.Instant;

public interface ProductRetailAccessProjectionWritePort {

    void refreshB2cTransferAccess(String passportId, String transferId, String sourceEventId, Instant updatedAt);
}
