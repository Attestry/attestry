package io.attestry.runtime.notificationoutbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxMetrics {

    private final Counter claimCounter;
    private final Counter publishCounter;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter publishSuccessStandaloneCounter;
    private final Counter publishFailureStandaloneCounter;
    private final Counter recoveredCounter;
    private final Counter processingTimeoutCounter;
    private final Timer claimTimer;
    private final Timer batchTimer;
    private final AtomicLong pendingSizeGauge;
    private final AtomicLong processingSizeGauge;
    private final AtomicLong failedSizeGauge;
    private final AtomicLong oldestPendingAgeSecondsGauge;
    private final AtomicLong oldestProcessingAgeSecondsGauge;

    NotificationOutboxMetrics(MeterRegistry meterRegistry) {
        this.claimCounter = Counter.builder("notification.outbox.claim.count")
            .register(meterRegistry);
        this.publishCounter = Counter.builder("notification.outbox.publish.count")
            .register(meterRegistry);
        this.publishSuccessCounter = Counter.builder("notification.outbox.publish.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("notification.outbox.publish.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.publishSuccessStandaloneCounter = Counter.builder("notification.outbox.publish.success.count")
            .register(meterRegistry);
        this.publishFailureStandaloneCounter = Counter.builder("notification.outbox.publish.failure.count")
            .register(meterRegistry);
        this.recoveredCounter = Counter.builder("notification.outbox.recovered.count")
            .register(meterRegistry);
        this.processingTimeoutCounter = Counter.builder("notification.outbox.processing.timeout.count")
            .register(meterRegistry);
        this.claimTimer = Timer.builder("notification.outbox.claim.duration")
            .register(meterRegistry);
        this.batchTimer = Timer.builder("notification.outbox.batch.duration")
            .register(meterRegistry);
        this.pendingSizeGauge = meterRegistry.gauge("notification.outbox.pending.size", new AtomicLong(0));
        this.processingSizeGauge = meterRegistry.gauge("notification.outbox.processing.size", new AtomicLong(0));
        this.failedSizeGauge = meterRegistry.gauge("notification.outbox.failed.size", new AtomicLong(0));
        this.oldestPendingAgeSecondsGauge = meterRegistry.gauge("notification.outbox.pending.oldest.age", new AtomicLong(0));
        this.oldestProcessingAgeSecondsGauge = meterRegistry.gauge("notification.outbox.processing.oldest.age", new AtomicLong(0));
    }

    void incrementClaimCount(int size) {
        claimCounter.increment(size);
    }

    void incrementPublishSuccessCount() {
        publishCounter.increment();
        publishSuccessCounter.increment();
        publishSuccessStandaloneCounter.increment();
    }

    void incrementPublishFailureCount() {
        publishCounter.increment();
        publishFailureCounter.increment();
        publishFailureStandaloneCounter.increment();
    }

    void incrementRecoveredCount(int size) {
        recoveredCounter.increment(size);
    }

    void incrementProcessingTimeoutCount(int size) {
        processingTimeoutCounter.increment(size);
    }

    <T> T recordClaim(Callable<T> callable) {
        try {
            return claimTimer.recordCallable(callable);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to record notification outbox claim metric", e);
        }
    }

    void recordBatch(Runnable runnable) {
        batchTimer.record(runnable);
    }

    void setPendingSize(long count) {
        pendingSizeGauge.set(count);
    }

    void setProcessingSize(long count) {
        processingSizeGauge.set(count);
    }

    void setFailedSize(long count) {
        failedSizeGauge.set(count);
    }

    void setOldestPendingAgeSeconds(long seconds) {
        oldestPendingAgeSecondsGauge.set(seconds);
    }

    void setOldestProcessingAgeSeconds(long seconds) {
        oldestProcessingAgeSecondsGauge.set(seconds);
    }
}
