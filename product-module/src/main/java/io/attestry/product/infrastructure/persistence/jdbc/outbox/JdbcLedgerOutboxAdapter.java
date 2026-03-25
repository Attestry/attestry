package io.attestry.product.infrastructure.persistence.jdbc.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.application.port.ledger.LedgerOutboxPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcLedgerOutboxAdapter implements LedgerOutboxPort {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public String enqueue(OutboxEventEnvelope event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new ProductDomainException(ProductErrorCode.OUTBOX_ENQUEUE_FAILED, "Failed to serialize ledger outbox payload");
        }

        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now(clock);
        entityManager.createNativeQuery("""
                INSERT INTO outbox_event (
                    event_id, aggregate_type, aggregate_id, event_type,
                    payload, idempotency_key, status, retry_count,
                    last_error, created_at, published_at
                ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)
                """)
            .setParameter(1, eventId)
            .setParameter(2, event.aggregateType())
            .setParameter(3, event.passportId())
            .setParameter(4, "LEDGER_APPEND")
            .setParameter(5, payload)
            .setParameter(6, event.idempotencyKey())
            .setParameter(7, "PENDING")
            .setParameter(8, 0)
            .setParameter(9, (String) null)
            .setParameter(10, now)
            .setParameter(11, (Instant) null)
            .executeUpdate();

        return eventId;
    }
}
