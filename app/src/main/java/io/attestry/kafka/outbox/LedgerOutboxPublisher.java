package io.attestry.kafka.outbox;

import io.attestry.config.KafkaProperties;
import io.attestry.kafka.outbox.persistence.OutboxEventJpaEntity;
import io.attestry.kafka.outbox.persistence.OutboxEventJpaRepository;
import io.attestry.kafka.outbox.persistence.OutboxStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxPublisher {

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final Clock clock;

    public LedgerOutboxPublisher(
        OutboxEventJpaRepository repository,
        KafkaTemplate<String, String> kafkaTemplate,
        KafkaProperties kafkaProperties,
        Clock clock
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        int batchSize = Math.max(1, kafkaProperties.getOutbox().getBatchSize());
        List<OutboxEventJpaEntity> pending = repository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.PENDING,
            PageRequest.of(0, batchSize)
        );

        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : pending) {
            try {
                kafkaTemplate.send(
                    kafkaProperties.getTopics().getLedgerOutbox(),
                    event.getAggregateId(),
                    event.getPayload()
                ).get();
                repository.save(new OutboxEventJpaEntity(
                    event.getEventId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getPayload(),
                    event.getIdempotencyKey(),
                    OutboxStatus.PUBLISHED,
                    event.getRetryCount(),
                    null,
                    event.getCreatedAt(),
                    Instant.now(clock)
                ));
            } catch (Exception ex) {
                int nextRetryCount = event.getRetryCount() + 1;
                OutboxStatus nextStatus = nextRetryCount >= kafkaProperties.getOutbox().getMaxRetries()
                    ? OutboxStatus.FAILED
                    : OutboxStatus.PENDING;
                repository.save(new OutboxEventJpaEntity(
                    event.getEventId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getPayload(),
                    event.getIdempotencyKey(),
                    nextStatus,
                    nextRetryCount,
                    trimError(ex.getMessage()),
                    event.getCreatedAt(),
                    event.getPublishedAt()
                ));
            }
        }
    }

    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
