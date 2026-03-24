package io.attestry.product.application.port.projection;

import java.time.Instant;

public interface ProductRetailAccessProjectionWritePort {

    void refreshB2cTransferAccess(RetailAccessPayload payload, String sourceEventId, Instant updatedAt);

    void syncPermissionAccess(String permissionId, String sourceEventId, Instant updatedAt);

    record RetailAccessPayload(
        String passportId,
        String transferId,
        String tenantId,
        Instant completedAt
    ) {
    }
}
