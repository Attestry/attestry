package io.attestry.product.application.port.ledger;

import io.attestry.product.domain.event.LedgerEventEnvelope;

public interface LedgerOutboxPort {
    String enqueue(LedgerEventEnvelope event);
}
