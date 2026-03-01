package io.attestry.ledger.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_chain")
public class LedgerChainJpaEntity {

    @Id
    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "last_seq")
    private Long lastSeq;

    @Column(name = "last_hash", length = 64)
    private String lastHash;

    protected LedgerChainJpaEntity() {
    }

    public LedgerChainJpaEntity(String passportId, Long lastSeq, String lastHash) {
        this.passportId = passportId;
        this.lastSeq = lastSeq;
        this.lastHash = lastHash;
    }

    public String getPassportId() {
        return passportId;
    }

    public Long getLastSeq() {
        return lastSeq;
    }

    public String getLastHash() {
        return lastHash;
    }
}
