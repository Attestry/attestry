package io.attestry.ledger.domain.ledger.model;

public record LedgerChainState(String passportId, long lastSeq, String lastHash) {
}
