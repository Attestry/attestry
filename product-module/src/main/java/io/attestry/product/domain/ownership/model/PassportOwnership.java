package io.attestry.product.domain.ownership.model;

import java.time.Instant;

public class PassportOwnership {

    private final String passportId;
    private String ownerId;
    private Instant updatedAt;
    private Integer lastLedgerSeq;

    private PassportOwnership(String passportId, String ownerId, Instant updatedAt, Integer lastLedgerSeq) {
        this.passportId = passportId;
        this.ownerId = ownerId;
        this.updatedAt = updatedAt;
        this.lastLedgerSeq = lastLedgerSeq;
    }

    public static PassportOwnership empty(String passportId) {
        return new PassportOwnership(passportId, null, Instant.now(), null);
    }

    public static PassportOwnership reconstitute(
        String passportId,
        String ownerId,
        Instant updatedAt,
        Integer lastLedgerSeq
    ) {
        return new PassportOwnership(passportId, ownerId, updatedAt, lastLedgerSeq);
    }

    public void updateOwner(String newOwnerId, int ledgerSeq, Instant now) {
        this.ownerId = newOwnerId;
        this.lastLedgerSeq = ledgerSeq;
        this.updatedAt = now;
    }

    public String getPassportId() { return passportId; }
    public String getOwnerId() { return ownerId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Integer getLastLedgerSeq() { return lastLedgerSeq; }
}
