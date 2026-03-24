package io.attestry.job.outbox.publish;

import io.attestry.job.outbox.model.*;
import io.attestry.job.outbox.repository.*;
import io.attestry.job.outbox.metrics.*;
import io.attestry.config.AppKafkaProperties;
import java.util.concurrent.CompletableFuture;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxTopicPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppKafkaProperties kafkaProperties;

    LedgerOutboxTopicPublisher(
        KafkaTemplate<String, String> kafkaTemplate,
        AppKafkaProperties kafkaProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public CompletableFuture<Void> publish(OutboxEventRecord event) {
        if (event.eventType() == OutboxEventType.LEDGER_APPEND) {
            CompletableFuture<?> ledgerFuture = kafkaTemplate.send(
                kafkaProperties.getTopics().getLedgerOutbox(),
                event.aggregateId(),
                event.payload()
            );

            if (event.aggregateType().isProjectionFanoutTarget()) {
                CompletableFuture<?> projectionFuture = kafkaTemplate.send(
                    kafkaProperties.getTopics().getProductProjection(),
                    event.aggregateId(),
                    event.payload()
                );
                return CompletableFuture.allOf(ledgerFuture, projectionFuture);
            }
            return CompletableFuture.allOf(ledgerFuture);
        }

        if (event.eventType() == OutboxEventType.PROJECTION_UPDATE) {
            CompletableFuture<?> projectionFuture = kafkaTemplate.send(
                kafkaProperties.getTopics().getProductProjection(),
                event.aggregateId(),
                event.payload()
            );
            return CompletableFuture.allOf(projectionFuture);
        }

        return CompletableFuture.completedFuture(null);
    }
}
