package io.attestry.ledger.domain.ledger.model;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.LedgerErrorCode;
import java.time.Instant;
import java.util.Map;

public record LedgerAppendInput(
    PassportId passportId,
    String eventCategory,
    String eventAction,
    String actorRole,
    String actorId,
    Instant occurredAt,
    Map<String, Object> payload,
    String idempotencyKey
) {

    public static LedgerAppendInput of(
        String passportId,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt,
        Map<String, Object> payload,
        String idempotencyKey
    ) {
        if (payload == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "payload is required");
        }
        if (occurredAt == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "occurredAt is required");
        }
        return new LedgerAppendInput(
            PassportId.of(passportId),
            requireText(eventCategory, "eventCategory"),
            requireText(eventAction, "eventAction"),
            requireText(actorRole, "actorRole"),
            requireText(actorId, "actorId"),
            occurredAt,
            payload,
            normalizeBlank(idempotencyKey)
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
