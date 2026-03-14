package io.attestry.ledger.application.ledger.verification;

public record LedgerVerificationResult(
    String passportId,
    boolean valid,
    long totalEntries,
    Long failedSeq,
    String reason,
    String latestEntryHash
) {
}
