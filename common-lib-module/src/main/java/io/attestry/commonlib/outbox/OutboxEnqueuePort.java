package io.attestry.commonlib.outbox;

public interface OutboxEnqueuePort {
    String enqueue(OutboxEventEnvelope event);
}
