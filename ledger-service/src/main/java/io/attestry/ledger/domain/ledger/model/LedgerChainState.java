package io.attestry.ledger.domain.ledger.model;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.LedgerErrorCode;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;
import java.time.Instant;

public record LedgerChainState(String passportId, long lastSeq, String lastHash) {

    public static LedgerChainState initialize(String passportId) {
        PassportId normalized = PassportId.of(passportId);
        return new LedgerChainState(normalized.value(), 0L, null);
    }

    public static LedgerChainState of(String passportId, Long lastSeq, String lastHash) {
        PassportId normalized = PassportId.of(passportId);
        long normalizedSeq = lastSeq == null ? 0L : lastSeq;
        if (normalizedSeq < 0L) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "lastSeq must be >= 0");
        }
        return new LedgerChainState(normalized.value(), normalizedSeq, normalizeHash(lastHash));
    }

    public PlannedAppend planNext(
        LedgerHashService hashService,
        String dataHash,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt
    ) {
        String normalizedDataHash = requireText(dataHash, "dataHash");
        String normalizedEventCategory = requireText(eventCategory, "eventCategory");
        String normalizedEventAction = requireText(eventAction, "eventAction");
        String normalizedActorRole = requireText(actorRole, "actorRole");
        String normalizedActorId = requireText(actorId, "actorId");
        if (occurredAt == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "occurredAt is required");
        }

        long nextSeq = lastSeq + 1L;
        String prevHash = lastHash;
        String entryHash = hashService.entryHash(
            prevHash,
            normalizedDataHash,
            nextSeq,
            normalizedEventCategory,
            normalizedEventAction,
            normalizedActorRole,
            normalizedActorId,
            occurredAt
        );
        LedgerChainState nextState = new LedgerChainState(passportId, nextSeq, entryHash);
        return new PlannedAppend(nextSeq, prevHash, entryHash, nextState);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeHash(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public record PlannedAppend(
        long seq,
        String prevHash,
        String entryHash,
        LedgerChainState nextState
    ) {
    }
}
