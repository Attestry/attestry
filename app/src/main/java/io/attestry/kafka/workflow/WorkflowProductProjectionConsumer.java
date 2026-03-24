package io.attestry.kafka.workflow;

import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort;
import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

    private final ProjectionEventParser eventParser;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkflowReadProjectionKafkaProperties kafkaProperties;
    private final Counter dlqCounter;
    private final Counter dlqPublishSuccessCounter;
    private final Counter dlqPublishFailureCounter;
    private final Counter consumeSuccessCounter;
    private final Counter consumeIgnoredCounter;
    private final Counter consumeFailureCounter;
    private final Clock clock;
    private final Timer projectionRefreshTimer;
    private final Timer projectionLagTimer;
    private final Timer dlqPublishTimer;
    private final List<ProjectionRefreshHandler> refreshHandlers;

    public WorkflowProductProjectionConsumer(
        com.fasterxml.jackson.databind.ObjectMapper objectMapper,
        WorkflowPassportProjectionWritePort projectionWriter,
        ProductShipmentProjectionWritePort shipmentProjectionWriter,
        ProductDistributionProjectionWritePort distributionProjectionWriter,
        ProductRetailAccessProjectionWritePort retailAccessProjectionWriter,
        KafkaTemplate<String, String> kafkaTemplate,
        WorkflowReadProjectionKafkaProperties kafkaProperties,
        MeterRegistry meterRegistry
    ) {
        this.eventParser = new ProjectionEventParser(objectMapper);
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = Clock.systemUTC();
        this.dlqCounter = Counter.builder("workflow.projection.dlq.count")
            .register(meterRegistry);
        this.dlqPublishSuccessCounter = Counter.builder("workflow.projection.dlq.publish.success.count")
            .register(meterRegistry);
        this.dlqPublishFailureCounter = Counter.builder("workflow.projection.dlq.publish.failure.count")
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
        this.dlqPublishTimer = Timer.builder("workflow.projection.dlq.publish.duration")
            .register(meterRegistry);
        this.refreshHandlers = List.of(
            new ProductProjectionRefreshHandler(projectionWriter, projectionRefreshTimer),
            new ShipmentProjectionRefreshHandler(shipmentProjectionWriter, projectionRefreshTimer),
            new DistributionProjectionRefreshHandler(distributionProjectionWriter, projectionRefreshTimer),
            new RetailAccessProjectionRefreshHandler(retailAccessProjectionWriter, projectionRefreshTimer)
        );
    }

    @KafkaListener(
        topics = "${app.workflow.read-projection.kafka.source-topic:product.projection.v1}",
        groupId = "${app.workflow.read-projection.consumer-group-id:workflow-product-read-projection-consumer}",
        concurrency = "${app.workflow.read-projection.listener-concurrency:1}"
    )
    public void consume(String payload) {
        try {
            ProjectionEventContext context = eventParser.parse(payload);
            if (context.passportId() == null || context.passportId().isBlank()) {
                consumeIgnoredCounter.increment();
                return;
            }

            projectionLagTimer.record(Duration.between(context.occurredAt(), Instant.now()).abs());

            if (refreshHandlers.stream().anyMatch(handler -> handler.refresh(context))) {
                consumeSuccessCounter.increment();
                return;
            }

            consumeIgnoredCounter.increment();
        } catch (Exception ex) {
            dlqCounter.increment();
            consumeFailureCounter.increment();
            publishToDlq(payload, ex);
            log.warn("workflow product projection DLQ: errorType={}, errorMessage={}",
                ex.getClass().getName(), ex.getMessage());
        }
    }

    private void publishToDlq(String payload, Exception ex) {
        ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
            kafkaProperties.getDlqTopic(), payload);
        dlqRecord.headers()
            .add("x-error-message", safeBytes(ex.getMessage()))
            .add("x-error-type", safeBytes(ex.getClass().getName()))
            .add("x-failed-at", safeBytes(Instant.now(clock).toString()))
            .add("x-consumer", safeBytes("workflow-product-projection"));

        Instant startedAt = Instant.now(clock);
        kafkaTemplate.send(dlqRecord).whenComplete((result, sendEx) -> {
            dlqPublishTimer.record(Duration.between(startedAt, Instant.now(clock)).abs());
            if (sendEx == null) {
                dlqPublishSuccessCounter.increment();
                return;
            }
            dlqPublishFailureCounter.increment();
            log.error("workflow product projection DLQ publish failed: errorType={}, errorMessage={}",
                sendEx.getClass().getName(), sendEx.getMessage());
        });
    }

    private byte[] safeBytes(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
