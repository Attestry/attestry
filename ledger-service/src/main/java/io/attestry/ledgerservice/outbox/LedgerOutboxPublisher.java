package io.attestry.ledgerservice.outbox;

import io.attestry.ledger.infrastructure.kafka.LedgerKafkaProperties;
import io.attestry.ledgerservice.outbox.persistence.LedgerOutboxEventJpaEntity;
import io.attestry.ledgerservice.outbox.persistence.LedgerOutboxEventJpaRepository;
import io.attestry.ledgerservice.outbox.persistence.LedgerOutboxStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.ledger.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxPublisher.class);
    private static final String EVENT_TYPE = "LEDGER_APPEND";
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);

    private final LedgerOutboxEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LedgerKafkaProperties kafkaProperties;
    private final Clock clock;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Timer publishTimer;
    private final AtomicLong pendingSizeGauge;
    private final AtomicLong failedSizeGauge;
    private final AtomicLong oldestPendingAgeSecondsGauge;

    public LedgerOutboxPublisher(
        LedgerOutboxEventJpaRepository repository,
        KafkaTemplate<String, String> kafkaTemplate,
        LedgerKafkaProperties kafkaProperties,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
        this.publishSuccessCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.publishTimer = Timer.builder("ledger.outbox.publish.duration")
            .register(meterRegistry);
        this.pendingSizeGauge = meterRegistry.gauge("ledger.outbox.pending.size", new AtomicLong(0));
        this.failedSizeGauge = meterRegistry.gauge("ledger.outbox.failed.size", new AtomicLong(0));
        this.oldestPendingAgeSecondsGauge = meterRegistry.gauge("ledger.outbox.pending.oldest.age", new AtomicLong(0));
    }

    @Scheduled(fixedDelayString = "${app.ledger.outbox.publish-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        refreshBacklogMetrics();
        publishTimer.record(this::doPublishPending);
        refreshBacklogMetrics();
    }

    private void doPublishPending() {
        Instant now = Instant.now(clock);
        int batchSize = Math.max(1, kafkaProperties.getOutbox().getBatchSize());
        List<LedgerOutboxEventJpaEntity> pending = repository.findRetryable(
            LedgerOutboxStatus.PENDING,
            now,
            PageRequest.of(0, batchSize)
        );

        for (LedgerOutboxEventJpaEntity event : pending) {
            try {
                publishToTopics(event);
                repository.save(new LedgerOutboxEventJpaEntity(
                    event.getEventId(),
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventType(),
                    event.getPayload(),
                    event.getIdempotencyKey(),
                    LedgerOutboxStatus.PUBLISHED,
                    event.getRetryCount(),
                    null,
                    event.getCreatedAt(),
                    Instant.now(clock),
                    null
                ));
                publishSuccessCounter.increment();
            } catch (Exception ex) {
                int nextRetryCount = event.getRetryCount() + 1;
                LedgerOutboxStatus nextStatus = nextRetryCount >= kafkaProperties.getOutbox().getMaxRetries()
                    ? LedgerOutboxStatus.FAILED
                    : LedgerOutboxStatus.PENDING;
                Instant nextRetryAt = nextStatus == LedgerOutboxStatus.PENDING
                    ? computeNextRetryAt(now, nextRetryCount)
                    : null;
                repository.save(new LedgerOutboxEventJpaEntity(
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
                if (nextStatus == LedgerOutboxStatus.FAILED) {
                    log.warn("ledger outbox event permanently failed: eventId={}, retryCount={}, lastError={}",
                        event.getEventId(), nextRetryCount, trimError(ex.getMessage()));
                }
            }
        }
    }

    private void publishToTopics(LedgerOutboxEventJpaEntity event) throws Exception {
        if ("LEDGER_APPEND".equals(event.getEventType())) {
            kafkaTemplate.send(
                kafkaProperties.getTopics().getLedgerOutbox(),
                event.getAggregateId(),
                event.getPayload()
            ).get();

            String aggregateType = event.getAggregateType();
            if ("PRODUCT".equals(aggregateType)
                || "SHIPMENT".equals(aggregateType)
                || "TRANSFER".equals(aggregateType)) {
                kafkaTemplate.send(
                    kafkaProperties.getTopics().getProductProjection(),
                    event.getAggregateId(),
                    event.getPayload()
                ).get();
            }
        }

        if ("PROJECTION_UPDATE".equals(event.getEventType())) {
            kafkaTemplate.send(
                kafkaProperties.getTopics().getProductProjection(),
                event.getAggregateId(),
                event.getPayload()
            ).get();
        }
    }

    private void refreshBacklogMetrics() {
        Instant now = Instant.now(clock);
        pendingSizeGauge.set(repository.countByEventTypeAndStatus(EVENT_TYPE, LedgerOutboxStatus.PENDING));
        failedSizeGauge.set(repository.countByEventTypeAndStatus(EVENT_TYPE, LedgerOutboxStatus.FAILED));
        long oldestAgeSeconds = repository.findFirstByEventTypeAndStatusOrderByCreatedAtAsc(EVENT_TYPE, LedgerOutboxStatus.PENDING)
            .map(event -> Math.max(0L, Duration.between(event.getCreatedAt(), now).getSeconds()))
            .orElse(0L);
        oldestPendingAgeSecondsGauge.set(oldestAgeSeconds);
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
