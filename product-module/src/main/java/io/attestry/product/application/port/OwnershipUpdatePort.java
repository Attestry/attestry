package io.attestry.product.application.port;

public interface OwnershipUpdatePort {

    void updateOwner(String passportId, String newOwnerId);
}
