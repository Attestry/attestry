package io.attestry.ledger.domain.ledger.model;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.LedgerErrorCode;

public record LedgerPayloadMaterialized(
    String payloadJson,
    String payloadCanonical,
    String dataHash
) {

    public LedgerPayloadMaterialized {
        payloadJson = requireText(payloadJson, "payloadJson");
        payloadCanonical = requireText(payloadCanonical, "payloadCanonical");
        dataHash = requireText(dataHash, "dataHash");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }
}
