package io.attestry.ledger.domain.ledger.model;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.LedgerErrorCode;

public record PassportId(String value) {

    public static PassportId of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "passportId is required");
        }
        return new PassportId(raw.trim());
    }
}
