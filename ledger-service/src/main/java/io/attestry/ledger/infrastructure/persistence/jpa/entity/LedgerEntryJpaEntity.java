package io.attestry.ledger.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ledger_entry", schema = "ledger")
public class LedgerEntryJpaEntity {

    @Id
    @Column(name = "ledger_id", nullable = false, length = 36)
    private String ledgerId;

    @Column(name = "passport_id", nullable = false, length = 36)
    private String passportId;

    @Column(name = "seq", nullable = false)
    private Long seq;

    @Column(name = "event_category", nullable = false, length = 50)
    private String eventCategory;

    @Column(name = "event_action", nullable = false, length = 50)
    private String eventAction;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "payload_canonical", columnDefinition = "TEXT")
    private String payloadCanonical;

    @Column(name = "data_hash", nullable = false, length = 64)
    private String dataHash;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "entry_hash", nullable = false, length = 64)
    private String entryHash;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    protected LedgerEntryJpaEntity() {
    }

    public LedgerEntryJpaEntity(
        String ledgerId,
        String passportId,
        Long seq,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt,
        Instant createdAt,
        String payloadJson,
        String payloadCanonical,
        String dataHash,
        String prevHash,
        String entryHash,
        String idempotencyKey,
        Integer schemaVersion
    ) {
        this.ledgerId = ledgerId;
        this.passportId = passportId;
        this.seq = seq;
        this.eventCategory = eventCategory;
        this.eventAction = eventAction;
        this.actorRole = actorRole;
        this.actorId = actorId;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.payloadJson = payloadJson;
        this.payloadCanonical = payloadCanonical;
        this.dataHash = dataHash;
        this.prevHash = prevHash;
        this.entryHash = entryHash;
        this.idempotencyKey = idempotencyKey;
        this.schemaVersion = schemaVersion;
    }

    public String getLedgerId() {
        return ledgerId;
    }

    public String getPassportId() {
        return passportId;
    }

    public Long getSeq() {
        return seq;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public String getEventAction() {
        return eventAction;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getActorId() {
        return actorId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getPayloadCanonical() {
        return payloadCanonical;
    }

    public String getDataHash() {
        return dataHash;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getEntryHash() {
        return entryHash;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }
}
