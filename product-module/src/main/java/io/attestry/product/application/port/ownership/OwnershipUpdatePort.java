package io.attestry.product.application.port.ownership;

public interface OwnershipUpdatePort {

    void updateOwner(String passportId, String newOwnerId);
}
