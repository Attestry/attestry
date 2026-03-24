package io.attestry.job.outbox.model;

import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxExecutionContextFactory {

    public LedgerOutboxExecutionContext createFor(Object owner) {
        return new LedgerOutboxExecutionContext(
            "publisher-" + Integer.toHexString(System.identityHashCode(owner))
        );
    }
}
