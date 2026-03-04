package io.attestry.workflow.infrastructure.persistence.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.workflow.application.port.TransferLedgerOutboxPort;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcTransferLedgerOutboxAdapter implements TransferLedgerOutboxPort {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcTransferLedgerOutboxAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public String enqueue(WorkflowLedgerEventEnvelope event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Failed to serialize transfer ledger payload");
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
            event.aggregateType(),
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
