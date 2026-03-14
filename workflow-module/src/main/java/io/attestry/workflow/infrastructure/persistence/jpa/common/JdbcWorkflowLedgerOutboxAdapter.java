package io.attestry.workflow.infrastructure.persistence.jpa.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcWorkflowLedgerOutboxAdapter implements WorkflowLedgerOutboxPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public String enqueue(OutboxEventEnvelope event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Failed to serialize workflow ledger payload");
        }

        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now(clock);
        jdbcTemplate.getJdbcOperations().update(
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
