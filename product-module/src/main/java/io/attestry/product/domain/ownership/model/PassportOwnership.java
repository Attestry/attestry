package io.attestry.product.domain.ownership.model;

import java.time.Instant;

public class PassportOwnership {

    private final String passportId;
    private String ownerId;
    private Instant updatedAt;

    private PassportOwnership(String passportId, String ownerId, Instant updatedAt) {
        this.passportId = passportId;
        this.ownerId = ownerId;
        this.updatedAt = updatedAt;
    }

    public static PassportOwnership empty(String passportId) {
        return new PassportOwnership(passportId, null, Instant.now());
    }

    public static PassportOwnership reconstitute(
        String passportId,
        String ownerId,
        Instant updatedAt
    ) {
        return new PassportOwnership(passportId, ownerId, updatedAt);
    }

    public void updateOwner(String newOwnerId, Instant now) {
        this.ownerId = newOwnerId;
        this.updatedAt = now;
    }

    public String getPassportId() { return passportId; }
    public String getOwnerId() { return ownerId; }
    public Instant getUpdatedAt() { return updatedAt; }
}
