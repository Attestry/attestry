package io.attestry.kafka.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.config.KafkaProperties;
import io.attestry.workflow.infrastructure.persistence.jpa.projection.WorkflowPassportProjectionWriter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowProductProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(WorkflowProductProjectionConsumer.class);

    private final ObjectMapper objectMapper;
    private final WorkflowPassportProjectionWriter projectionWriter;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final Counter dlqCounter;

    public WorkflowProductProjectionConsumer(
        ObjectMapper objectMapper,
        WorkflowPassportProjectionWriter projectionWriter,
        KafkaTemplate<String, String> kafkaTemplate,
        KafkaProperties kafkaProperties,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.projectionWriter = projectionWriter;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.dlqCounter = Counter.builder("workflow.projection.dlq.count")
            .register(meterRegistry);
    }

    @KafkaListener(
        topics = "${app.kafka.topics.ledger-outbox}",
        groupId = "${app.workflow.read-projection.consumer-group-id:workflow-product-read-projection-consumer}"
    )
    public void consume(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!"PRODUCT".equals(root.path("aggregateType").asText())) {
                return;
            }

            String passportId = text(root, "passportId");
            if (passportId == null || passportId.isBlank()) {
                return;
            }

            String eventCategory = text(root, "eventCategory");
            String eventAction = text(root, "eventAction");
            Instant occurredAt = root.hasNonNull("occurredAt")
                ? Instant.parse(root.get("occurredAt").asText())
                : Instant.now();
            String idempotencyKey = text(root, "idempotencyKey");

            if (shouldRefreshStateAndCatalog(eventCategory, eventAction)) {
                projectionWriter.refreshStateAndCatalog(passportId, safeEventId(idempotencyKey, passportId, eventAction), null, occurredAt);
                log.debug("workflow product projection refreshed: passportId={}, eventCategory={}, eventAction={}",
                    passportId, eventCategory, eventAction);
            }
        } catch (Exception ex) {
            dlqCounter.increment();
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
                kafkaProperties.getTopics().getLedgerDlq(), payload);
            dlqRecord.headers()
                .add("x-error-message", safeBytes(ex.getMessage()))
                .add("x-error-type", safeBytes(ex.getClass().getName()))
                .add("x-failed-at", safeBytes(Instant.now().toString()))
                .add("x-consumer", safeBytes("workflow-product-projection"));
            kafkaTemplate.send(dlqRecord);
            log.warn("workflow product projection DLQ: errorType={}, errorMessage={}",
                ex.getClass().getName(), ex.getMessage());
        }
    }

    private boolean shouldRefreshStateAndCatalog(String eventCategory, String eventAction) {
        if ("GENESIS".equals(eventCategory) && "MINTED".equals(eventAction)) {
            return true;
        }
        if ("LIFECYCLE".equals(eventCategory) && "VOIDED".equals(eventAction)) {
            return true;
        }
        return "RISK".equals(eventCategory);
    }

    private String text(JsonNode root, String fieldName) {
        return root.hasNonNull(fieldName) ? root.get(fieldName).asText() : null;
    }

    private String safeEventId(String idempotencyKey, String passportId, String eventAction) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKey;
        }
        return "consumer:" + passportId + ":" + (eventAction == null ? "UNKNOWN" : eventAction);
    }

    private byte[] safeBytes(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
