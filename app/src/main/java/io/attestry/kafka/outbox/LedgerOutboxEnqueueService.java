package io.attestry.kafka.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.kafka.outbox.persistence.OutboxEventJpaEntity;
import io.attestry.kafka.outbox.persistence.OutboxEventJpaRepository;
import io.attestry.kafka.outbox.persistence.OutboxStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerOutboxEnqueueService {

    private final OutboxEventJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LedgerOutboxEnqueueService(OutboxEventJpaRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public String enqueue(OutboxEventEnvelope payload) {
        validate(payload);
        String eventId = UUID.randomUUID().toString();
        String idempotencyKey = normalizeBlank(payload.idempotencyKey());
        String serialized = write(payload);
        repository.save(new OutboxEventJpaEntity(
            eventId,
            "PASSPORT",
            payload.passportId(),
            "LEDGER_APPEND",
            serialized,
            idempotencyKey,
            OutboxStatus.PENDING,
            0,
            null,
            Instant.now(clock),
            null,
            null
        ));
        return eventId;
    }

    private String write(OutboxEventEnvelope payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("outbox payload serialization failed", ex);
        }
    }

    private void validate(OutboxEventEnvelope payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload is required");
        }
        requireText(payload.passportId(), "passportId");
        requireText(payload.eventCategory(), "eventCategory");
        requireText(payload.eventAction(), "eventAction");
        requireText(payload.actorRole(), "actorRole");
        requireText(payload.actorId(), "actorId");
        if (payload.payload() == null) {
            throw new IllegalArgumentException("payload.payload is required");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private String normalizeBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
