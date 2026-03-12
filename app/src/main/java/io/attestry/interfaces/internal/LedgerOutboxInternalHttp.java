package io.attestry.interfaces.internal;

import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.kafka.outbox.LedgerOutboxEnqueueService;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ledger/outbox")
public class LedgerOutboxInternalHttp {

    private final LedgerOutboxEnqueueService enqueueService;

    public LedgerOutboxInternalHttp(LedgerOutboxEnqueueService enqueueService) {
        this.enqueueService = enqueueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnqueueResponse enqueue(@RequestBody EnqueueRequest request) {
        String eventId = enqueueService.enqueue(new OutboxEventEnvelope(
            request.aggregateType(),
            request.passportId(),
            request.eventCategory(),
            request.eventAction(),
            request.actorRole(),
            request.actorId(),
            request.occurredAt(),
            request.payload(),
            request.idempotencyKey()
        ));
        return new EnqueueResponse(eventId);
    }

    public record EnqueueRequest(
        String aggregateType,
        String passportId,
        String eventCategory,
        String eventAction,
        String actorRole,
        String actorId,
        Instant occurredAt,
        Map<String, Object> payload,
        String idempotencyKey
    ) {
    }

    public record EnqueueResponse(String eventId) {
    }
}
