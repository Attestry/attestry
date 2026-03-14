package io.attestry.ledger.domain.ledger.model;

import io.attestry.ledger.domain.LedgerDomainException;
import io.attestry.ledger.domain.LedgerErrorCode;
import io.attestry.ledger.domain.ledger.service.LedgerHashService;

public record LedgerChain(LedgerChainState state) {

    public static LedgerChain initialize(String passportId) {
        return new LedgerChain(LedgerChainState.initialize(passportId));
    }

    public static LedgerChain restore(String passportId, Long lastSeq, String lastHash) {
        return new LedgerChain(LedgerChainState.of(passportId, lastSeq, lastHash));
    }

    public AppendPlan append(
        LedgerAppendInput input,
        LedgerPayloadMaterialized materialized,
        LedgerHashService hashService,
        int schemaVersion
    ) {
        if (!state.passportId().equals(input.passportId().value())) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_LEDGER_REQUEST, "passportId mismatch between chain and input");
        }
        LedgerChainState.PlannedAppend planned = state.planNext(
            hashService,
            materialized.dataHash(),
            input.eventCategory(),
            input.eventAction(),
            input.actorRole(),
            input.actorId(),
            input.occurredAt()
        );
        LedgerEntry entry = LedgerEntry.append(
            input.passportId(),
            planned,
            input.eventCategory(),
            input.eventAction(),
            input.actorRole(),
            input.actorId(),
            input.occurredAt(),
            materialized.payloadJson(),
            materialized.payloadCanonical(),
            materialized.dataHash(),
            input.idempotencyKey(),
            schemaVersion
        );
        return new AppendPlan(entry, new LedgerChain(planned.nextState()));
    }

    public record AppendPlan(
        LedgerEntry entry,
        LedgerChain nextChain
    ) {
    }
}
