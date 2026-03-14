package io.attestry.kafka.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort;
import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final WorkflowPassportProjectionWritePort projectionWriter;
    private final ProductShipmentProjectionWritePort shipmentProjectionWriter;
    private final ProductDistributionProjectionWritePort distributionProjectionWriter;
    private final ProductRetailAccessProjectionWritePort retailAccessProjectionWriter;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkflowReadProjectionKafkaProperties kafkaProperties;
    private final Counter dlqCounter;
    private final Counter consumeSuccessCounter;
    private final Counter consumeIgnoredCounter;
    private final Counter consumeFailureCounter;
    private final Timer projectionRefreshTimer;
    private final Timer projectionLagTimer;

    public WorkflowProductProjectionConsumer(
        ObjectMapper objectMapper,
        WorkflowPassportProjectionWritePort projectionWriter,
        ProductShipmentProjectionWritePort shipmentProjectionWriter,
        ProductDistributionProjectionWritePort distributionProjectionWriter,
        ProductRetailAccessProjectionWritePort retailAccessProjectionWriter,
        KafkaTemplate<String, String> kafkaTemplate,
        WorkflowReadProjectionKafkaProperties kafkaProperties,
        MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.projectionWriter = projectionWriter;
        this.shipmentProjectionWriter = shipmentProjectionWriter;
        this.distributionProjectionWriter = distributionProjectionWriter;
        this.retailAccessProjectionWriter = retailAccessProjectionWriter;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.dlqCounter = Counter.builder("workflow.projection.dlq.count")
            .register(meterRegistry);
        this.consumeSuccessCounter = Counter.builder("workflow.projection.consume.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.consumeIgnoredCounter = Counter.builder("workflow.projection.consume.count")
            .tag("result", "ignored")
            .register(meterRegistry);
        this.consumeFailureCounter = Counter.builder("workflow.projection.consume.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.projectionRefreshTimer = Timer.builder("workflow.projection.refresh.duration")
            .register(meterRegistry);
        this.projectionLagTimer = Timer.builder("workflow.projection.lag")
            .register(meterRegistry);
    }

    @KafkaListener(
        topics = "${app.workflow.read-projection.kafka.source-topic:product.projection.v1}",
        groupId = "${app.workflow.read-projection.consumer-group-id:workflow-product-read-projection-consumer}",
        concurrency = "${app.workflow.read-projection.listener-concurrency:1}"
    )
    public void consume(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String aggregateType = root.path("aggregateType").asText();

            String passportId = text(root, "passportId");
            if (passportId == null || passportId.isBlank()) {
                consumeIgnoredCounter.increment();
                return;
            }

            String eventCategory = text(root, "eventCategory");
            String eventAction = text(root, "eventAction");
            Instant occurredAt = root.hasNonNull("occurredAt")
                ? Instant.parse(root.get("occurredAt").asText())
                : Instant.now();
            String idempotencyKey = text(root, "idempotencyKey");
            String sourceEventId = safeEventId(idempotencyKey, passportId, eventAction);
            projectionLagTimer.record(Duration.between(occurredAt, Instant.now()).abs());

            if ("PRODUCT".equals(aggregateType) && shouldRefreshStateAndCatalog(eventCategory, eventAction)) {
                projectionRefreshTimer.record(() ->
                    projectionWriter.refreshStateAndCatalog(passportId, sourceEventId, null, occurredAt)
                );
                consumeSuccessCounter.increment();
                log.debug("workflow product projection refreshed: passportId={}, eventCategory={}, eventAction={}",
                    passportId, eventCategory, eventAction);
                return;
            }

            if ("SHIPMENT".equals(aggregateType) && "SHIPMENT".equals(eventCategory)) {
                projectionRefreshTimer.record(() ->
                    shipmentProjectionWriter.refreshShipmentProjection(passportId, sourceEventId, null, occurredAt)
                );
                consumeSuccessCounter.increment();
                log.debug("shipment projection refreshed: passportId={}, eventAction={}", passportId, eventAction);
                return;
            }

            if ("DISTRIBUTION".equals(aggregateType) && "DISTRIBUTION".equals(eventCategory)) {
                projectionRefreshTimer.record(() ->
                    distributionProjectionWriter.refreshDistributionProjection(passportId, sourceEventId, null, occurredAt)
                );
                consumeSuccessCounter.increment();
                log.debug("distribution projection refreshed: passportId={}, eventAction={}", passportId, eventAction);
                return;
            }

            if ("TRANSFER".equals(aggregateType) && "OWNERSHIP".equals(eventCategory) && "CLAIMED".equals(eventAction)) {
                String transferId = text(root.path("payload"), "transferId");
                if (transferId != null && !transferId.isBlank()) {
                    projectionRefreshTimer.record(() ->
                        retailAccessProjectionWriter.refreshB2cTransferAccess(passportId, transferId, sourceEventId, occurredAt)
                    );
                    consumeSuccessCounter.increment();
                    log.debug("retail access projection refreshed: passportId={}, transferId={}", passportId, transferId);
                    return;
                }
            }

            consumeIgnoredCounter.increment();
        } catch (Exception ex) {
            dlqCounter.increment();
            consumeFailureCounter.increment();
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
                kafkaProperties.getDlqTopic(), payload);
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
