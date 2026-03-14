package io.attestry.ledger.domain.ledger.model;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.LedgerErrorCode;

public record LedgerId(String value) {

    public static LedgerId of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "ledgerId is required");
        }
        return new LedgerId(raw.trim());
    }
}
