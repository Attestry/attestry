package io.attestry.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/experiments/outbox")
@Profile({"local", "dev"})
public class OutboxExperimentHttp {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxExperimentHttp(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @PostMapping("/seed")
    public SeedResponse seed(@RequestBody SeedRequest request) {
        int count = Math.max(1, request.count());
        int aggregateCount = Math.max(1, request.aggregateCount());
        String aggregateType = request.aggregateType() != null ? request.aggregateType() : "SHIPMENT";
        String eventType = request.eventType() != null ? request.eventType() : "LEDGER_APPEND";
        String runId = request.runId() != null ? request.runId() : "run-" + System.currentTimeMillis();
        boolean sameAggregateBurst = request.sameAggregateBurst();

        String[] aggregateIds = new String[aggregateCount];
        for (int i = 0; i < aggregateCount; i++) {
            aggregateIds[i] = request.fixedAggregatePrefix() != null
                ? request.fixedAggregatePrefix() + "-" + i
                : UUID.randomUUID().toString();
        }

        int inserted = 0;
        for (int i = 0; i < count; i++) {
            String aggregateId = sameAggregateBurst
                ? aggregateIds[0]
                : aggregateIds[i % aggregateCount];
            String eventId = UUID.randomUUID().toString();
            Instant now = Instant.now(clock);

            String payload;
            try {
                payload = objectMapper.writeValueAsString(Map.of(
                    "aggregateType", aggregateType,
                    "passportId", aggregateId,
                    "eventCategory", "EXPERIMENT",
                    "eventAction", "SEED",
                    "actorRole", "SYSTEM",
                    "actorId", "experiment",
                    "occurredAt", now.toString(),
                    "payload", Map.of(
                        "runId", runId,
                        "seqInRun", i,
                        "totalInRun", count
                    ),
                    "idempotencyKey", eventId
                ));
            } catch (Exception e) {
                payload = "{}";
            }

            jdbcTemplate.update(
                """
                    INSERT INTO outbox_event (
                        event_id, aggregate_type, aggregate_id, event_type,
                        payload, idempotency_key, status, retry_count,
                        last_error, created_at, published_at
                    ) VALUES (?, ?, ?, ?, ?, ?, 'PENDING', 0, NULL, ?, NULL)
                """,
                eventId, aggregateType, aggregateId, eventType,
                payload, eventId, Timestamp.from(now)
            );
            inserted++;
        }

        return new SeedResponse(inserted, aggregateCount, aggregateIds, runId);
    }

    @PostMapping("/verify-ordering")
    public OrderingVerificationResponse verifyOrdering() {
        List<OrderingViolation> violations = jdbcTemplate.query(
            """
                SELECT e.event_id, e.aggregate_id, e.created_at, e.published_at
                FROM outbox_event e
                WHERE e.status = 'PUBLISHED'
                  AND e.event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                  AND e.published_at IS NOT NULL
                  AND EXISTS (
                      SELECT 1 FROM outbox_event later
                      WHERE later.aggregate_id = e.aggregate_id
                        AND later.event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                        AND later.status = 'PUBLISHED'
                        AND later.created_at > e.created_at
                        AND later.published_at < e.published_at
                  )
            """,
            (rs, rowNum) -> new OrderingViolation(
                rs.getString("event_id"),
                rs.getString("aggregate_id"),
                rs.getTimestamp("created_at").toInstant().toString(),
                rs.getTimestamp("published_at").toInstant().toString()
            )
        );
        return new OrderingVerificationResponse(violations.size(), violations);
    }

    @PostMapping("/status")
    public OutboxStatusResponse status() {
        List<StatusCount> counts = jdbcTemplate.query(
            """
                SELECT status, COUNT(*) AS cnt
                FROM outbox_event
                WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                GROUP BY status
                ORDER BY status
            """,
            (rs, rowNum) -> new StatusCount(rs.getString("status"), rs.getLong("cnt"))
        );
        return new OutboxStatusResponse(counts);
    }

    record SeedRequest(
        int count,
        int aggregateCount,
        String aggregateType,
        String eventType,
        String runId,
        boolean sameAggregateBurst,
        String fixedAggregatePrefix
    ) {}

    record SeedResponse(int inserted, int aggregateCount, String[] aggregateIds, String runId) {}
    record OrderingViolation(String eventId, String aggregateId, String createdAt, String publishedAt) {}
    record OrderingVerificationResponse(int violationCount, List<OrderingViolation> violations) {}
    record StatusCount(String status, long count) {}
    record OutboxStatusResponse(List<StatusCount> counts) {}
}
