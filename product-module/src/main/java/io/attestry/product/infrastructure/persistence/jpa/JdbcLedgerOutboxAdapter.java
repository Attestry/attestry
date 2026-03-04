package io.attestry.product.infrastructure.persistence.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.product.application.port.LedgerOutboxPort;
import io.attestry.product.domain.event.LedgerEventEnvelope;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcLedgerOutboxAdapter implements LedgerOutboxPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcLedgerOutboxAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public String enqueue(LedgerEventEnvelope event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new ProductDomainException(ProductErrorCode.OUTBOX_ENQUEUE_FAILED, "Failed to serialize ledger outbox payload");
        }

        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now(clock);
        jdbcTemplate.update(
            """
                INSERT INTO outbox_event (
                    event_id,
                    aggregate_type,
                    aggregate_id,
                    event_type,
                    payload,
                    idempotency_key,
                    status,
                    retry_count,
                    last_error,
                    created_at,
                    published_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            eventId,
            "PASSPORT",
            event.passportId(),
            "LEDGER_APPEND",
            payload,
            event.idempotencyKey(),
            "PENDING",
            0,
            null,
            Timestamp.from(now),
            null
        );
        return eventId;
    }
}
