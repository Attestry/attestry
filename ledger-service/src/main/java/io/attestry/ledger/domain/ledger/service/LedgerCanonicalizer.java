package io.attestry.ledger.domain.ledger.service;

import java.util.Map;

public interface LedgerCanonicalizer {
    String canonicalize(Map<String, Object> payload);

    String serialize(Map<String, Object> payload);
}
