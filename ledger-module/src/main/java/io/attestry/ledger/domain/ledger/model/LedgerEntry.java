package io.attestry.ledger.domain.ledger.model;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.LedgerErrorCode;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
    String ledgerId,
    String passportId,
    long seq,
    String eventCategory,
    String eventAction,
    String actorRole,
    String actorId,
    Instant occurredAt,
    String payloadJson,
    String payloadCanonical,
    String dataHash,
    String prevHash,
    String entryHash,
    String idempotencyKey,
    int schemaVersion
) {

    public LedgerEntry {
        ledgerId = requireText(ledgerId, "ledgerId");
        passportId = PassportId.of(passportId).value();
        if (seq <= 0L) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "seq must be > 0");
        }
        eventCategory = requireText(eventCategory, "eventCategory");
        eventAction = requireText(eventAction, "eventAction");
        actorRole = requireText(actorRole, "actorRole");
        actorId = requireText(actorId, "actorId");
        if (occurredAt == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "occurredAt is required");
        }
        payloadJson = requireText(payloadJson, "payloadJson");
        payloadCanonical = requireText(payloadCanonical, "payloadCanonical");
        dataHash = requireText(dataHash, "dataHash");
        entryHash = requireText(entryHash, "entryHash");
        idempotencyKey = normalizeBlank(idempotencyKey);
        if (schemaVersion <= 0) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "schemaVersion must be > 0");
        }
    }

    public static LedgerEntry append(
        PassportId passportId,
        LedgerChainState.PlannedAppend plannedAppend,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt,
        String payloadJson,
        String payloadCanonical,
        String dataHash,
        String idempotencyKey,
        int schemaVersion
    ) {
        return new LedgerEntry(
            UUID.randomUUID().toString(),
            passportId.value(),
            plannedAppend.seq(),
            eventCategory,
            eventAction,
            actorRole,
            actorId,
            occurredAt,
            payloadJson,
            payloadCanonical,
            dataHash,
            plannedAppend.prevHash(),
            plannedAppend.entryHash(),
            idempotencyKey,
            schemaVersion
        );
    }

    public static LedgerEntry rehydrate(
        String ledgerId,
        String passportId,
        long seq,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt,
        String payloadJson,
        String payloadCanonical,
        String dataHash,
        String prevHash,
        String entryHash,
        String idempotencyKey,
        int schemaVersion
    ) {
        return new LedgerEntry(
            ledgerId,
            passportId,
            seq,
            eventCategory,
            eventAction,
            actorRole,
            actorId,
            occurredAt,
            payloadJson,
            payloadCanonical,
            dataHash,
            prevHash,
            entryHash,
            idempotencyKey,
            schemaVersion
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeBlank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
