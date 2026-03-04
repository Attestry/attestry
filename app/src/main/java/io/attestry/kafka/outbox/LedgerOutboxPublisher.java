package io.attestry.kafka.outbox;

import io.attestry.config.KafkaProperties;
import io.attestry.kafka.outbox.persistence.OutboxEventJpaEntity;
import io.attestry.kafka.outbox.persistence.OutboxEventJpaRepository;
import io.attestry.kafka.outbox.persistence.OutboxStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxPublisher.class);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);

    private final OutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final Clock clock;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Timer publishTimer;

    public LedgerOutboxPublisher(
        OutboxEventJpaRepository repository,
        KafkaTemplate<String, String> kafkaTemplate,
        KafkaProperties kafkaProperties,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
        this.publishSuccessCounter = Counter.builder("outbox.publish.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("outbox.publish.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.publishTimer = Timer.builder("outbox.publish.duration")
            .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        publishTimer.record(this::doPublishPending);
    }

    private void doPublishPending() {
        Instant now = Instant.now(clock);
        int batchSize = Math.max(1, kafkaProperties.getOutbox().getBatchSize());
        List<OutboxEventJpaEntity> pending = repository.findRetryable(
            OutboxStatus.PENDING,
            now,
            PageRequest.of(0, batchSize)
        );

        if (pending.isEmpty()) {
            return;
        }

        int publishedCount = 0;
        int failedCount = 0;

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
                    Instant.now(clock),
                    null
                ));
                publishSuccessCounter.increment();
                publishedCount++;
            } catch (Exception ex) {
                int nextRetryCount = event.getRetryCount() + 1;
                OutboxStatus nextStatus = nextRetryCount >= kafkaProperties.getOutbox().getMaxRetries()
                    ? OutboxStatus.FAILED
                    : OutboxStatus.PENDING;
                Instant nextRetryAt = nextStatus == OutboxStatus.PENDING
                    ? computeNextRetryAt(now, nextRetryCount)
                    : null;
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
                    event.getPublishedAt(),
                    nextRetryAt
                ));
                publishFailureCounter.increment();
                failedCount++;

                if (nextStatus == OutboxStatus.FAILED) {
                    log.warn("outbox event permanently failed: eventId={}, retryCount={}, lastError={}",
                        event.getEventId(), nextRetryCount, trimError(ex.getMessage()));
                }
            }
        }

        log.debug("outbox publish batch: batchSize={}, publishedCount={}, failedCount={}",
            pending.size(), publishedCount, failedCount);
    }

    private Instant computeNextRetryAt(Instant now, int retryCount) {
        long delaySeconds = BASE_DELAY.toSeconds() * (1L << Math.min(retryCount, 10));
        Duration delay = Duration.ofSeconds(Math.min(delaySeconds, MAX_BACKOFF.toSeconds()));
        return now.plus(delay);
    }

    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
