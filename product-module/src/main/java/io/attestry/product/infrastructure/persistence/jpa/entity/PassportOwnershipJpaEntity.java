package io.attestry.product.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "passport_ownership")
public class PassportOwnershipJpaEntity {

    @Id
    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "owner_id", length = 36)
    private String ownerId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_ledger_seq")
    private Integer lastLedgerSeq;

    protected PassportOwnershipJpaEntity() {
    }

    public PassportOwnershipJpaEntity(String passportId, String ownerId, Instant updatedAt, Integer lastLedgerSeq) {
        this.passportId = passportId;
        this.ownerId = ownerId;
        this.updatedAt = updatedAt;
        this.lastLedgerSeq = lastLedgerSeq;
    }

    public String getPassportId() { return passportId; }
    public String getOwnerId() { return ownerId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Integer getLastLedgerSeq() { return lastLedgerSeq; }
}
