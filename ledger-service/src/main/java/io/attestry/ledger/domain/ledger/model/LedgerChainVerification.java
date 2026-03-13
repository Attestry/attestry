package io.attestry.ledger.domain.ledger.model;

public record LedgerChainVerification(
    PassportId passportId,
    boolean valid,
    long totalEntries,
    Long failedSeq,
    String reason,
    String latestEntryHash
) {
}
