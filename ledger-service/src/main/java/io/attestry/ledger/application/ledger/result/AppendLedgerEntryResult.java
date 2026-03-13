package io.attestry.ledger.application.ledger.result;

public record AppendLedgerEntryResult(
    String ledgerId,
    String passportId,
    long seq,
    String dataHash,
    String prevHash,
    String entryHash,
    String idempotencyKey,
    boolean duplicated
) {
}
