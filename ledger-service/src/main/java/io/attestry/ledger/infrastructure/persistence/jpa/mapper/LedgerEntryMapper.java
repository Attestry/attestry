package io.attestry.ledger.infrastructure.persistence.jpa.mapper;

import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import io.attestry.ledger.infrastructure.persistence.jpa.entity.LedgerEntryJpaEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryMapper {

    public LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return LedgerEntry.rehydrate(
            entity.getLedgerId(),
            entity.getPassportId(),
            entity.getSeq(),
            entity.getEventCategory(),
            entity.getEventAction(),
            entity.getActorRole(),
            entity.getActorId(),
            entity.getOccurredAt(),
            entity.getPayloadJson(),
            entity.getPayloadCanonical(),
            entity.getDataHash(),
            entity.getPrevHash(),
            entity.getEntryHash(),
            entity.getIdempotencyKey(),
            entity.getSchemaVersion()
        );
    }

    public LedgerEntryJpaEntity toEntity(LedgerEntry entry, Instant createdAt) {
        if (entry == null) {
            return null;
        }
        return new LedgerEntryJpaEntity(
            entry.ledgerId(),
            entry.passportId(),
            entry.seq(),
            entry.eventCategory(),
            entry.eventAction(),
            entry.actorRole(),
            entry.actorId(),
            entry.occurredAt(),
            createdAt,
            entry.payloadJson(),
            entry.payloadCanonical(),
            entry.dataHash(),
            entry.prevHash(),
            entry.entryHash(),
            entry.idempotencyKey(),
            entry.schemaVersion()
        );
    }
}
