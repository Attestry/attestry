package io.attestry.ledger.domain.ledger.service;

import io.attestry.ledger.domain.ledger.model.LedgerAppendInput;
import io.attestry.ledger.domain.ledger.model.LedgerPayloadMaterialized;

public class LedgerAppendDomainService {

    private final LedgerCanonicalizer canonicalizer;
    private final LedgerHashService hashService;

    public LedgerAppendDomainService(
        LedgerCanonicalizer canonicalizer,
        LedgerHashService hashService
    ) {
        this.canonicalizer = canonicalizer;
        this.hashService = hashService;
    }

    public LedgerPayloadMaterialized materialize(LedgerAppendInput input) {
        String payloadCanonical = canonicalizer.canonicalize(input.payload());
        String payloadJson = canonicalizer.serialize(input.payload());
        String dataHash = hashService.dataHash(payloadCanonical);
        return new LedgerPayloadMaterialized(payloadJson, payloadCanonical, dataHash);
    }
}
