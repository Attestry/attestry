package io.attestry.ledger.application.port;

import io.attestry.ledger.domain.ledger.model.LedgerEntry;
import java.time.Instant;

public interface LedgerAppendRepositoryPort {

    AppendOutcome append(AppendRequest request);

    record AppendRequest(
        String passportId,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt,
        String payloadJson,
        String payloadCanonical,
        String dataHash,
        String idempotencyKey
    ) {
    }

    record AppendOutcome(LedgerEntry entry, boolean duplicated) {
    }
}
