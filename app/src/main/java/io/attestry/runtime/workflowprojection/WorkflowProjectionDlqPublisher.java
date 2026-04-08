package io.attestry.runtime.workflowprojection;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class WorkflowProjectionDlqPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkflowProjectionDlqPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkflowReadProjectionKafkaProperties kafkaProperties;
    private final WorkflowProjectionMetrics metrics;

    WorkflowProjectionDlqPublisher(
        KafkaTemplate<String, String> kafkaTemplate,
        WorkflowReadProjectionKafkaProperties kafkaProperties,
        WorkflowProjectionMetrics metrics
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.metrics = metrics;
    }

    void publish(String payload, Exception ex) {
        ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(kafkaProperties.getDlqTopic(), payload);
        dlqRecord.headers()
            .add("x-error-message", safeBytes(ex.getMessage()))
            .add("x-error-type", safeBytes(ex.getClass().getName()))
            .add("x-failed-at", safeBytes(Instant.now(metrics.clock()).toString()))
            .add("x-consumer", safeBytes("workflow-product-projection"));

        Instant startedAt = Instant.now(metrics.clock());
        kafkaTemplate.send(dlqRecord).whenComplete((result, sendEx) -> {
            Duration duration = Duration.between(startedAt, Instant.now(metrics.clock()));
            if (sendEx == null) {
                metrics.recordDlqPublish(duration, true);
                return;
            }
            metrics.recordDlqPublish(duration, false);
            log.error("workflow product projection DLQ publish failed: errorType={}, errorMessage={}",
                sendEx.getClass().getName(), sendEx.getMessage());
        });
    }

    private byte[] safeBytes(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
    }
}
