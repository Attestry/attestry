package io.attestry.runtime.ledgeroutbox.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxMetrics {

    private final Counter claimCounter;
    private final Counter publishCounter;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter publishSuccessStandaloneCounter;
    private final Counter publishFailureStandaloneCounter;
    private final Timer claimTimer;
    private final Timer publishTimer;
    private final Timer finalizeTimer;
    private final Timer batchTimer;
    private final AtomicLong pendingSizeGauge;
    private final AtomicLong processingSizeGauge;
    private final AtomicLong failedSizeGauge;
    private final AtomicLong oldestPendingAgeSecondsGauge;

    public LedgerOutboxMetrics(MeterRegistry meterRegistry) {
        this.claimCounter = Counter.builder("ledger.outbox.claim.count")
            .register(meterRegistry);
        this.publishCounter = Counter.builder("ledger.outbox.publish.count")
            .register(meterRegistry);
        this.publishSuccessCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.publishSuccessStandaloneCounter = Counter.builder("ledger.outbox.publish.success.count")
            .register(meterRegistry);
        this.publishFailureStandaloneCounter = Counter.builder("ledger.outbox.publish.failure.count")
            .register(meterRegistry);
        this.claimTimer = Timer.builder("ledger.outbox.claim.duration")
            .register(meterRegistry);
        this.publishTimer = Timer.builder("ledger.outbox.publish.duration")
            .register(meterRegistry);
        this.finalizeTimer = Timer.builder("ledger.outbox.finalize.duration")
            .register(meterRegistry);
        this.batchTimer = Timer.builder("ledger.outbox.batch.duration")
            .register(meterRegistry);
        this.pendingSizeGauge = meterRegistry.gauge("ledger.outbox.pending.size", new AtomicLong(0));
        this.processingSizeGauge = meterRegistry.gauge("ledger.outbox.processing.size", new AtomicLong(0));
        this.failedSizeGauge = meterRegistry.gauge("ledger.outbox.failed.size", new AtomicLong(0));
        this.oldestPendingAgeSecondsGauge = meterRegistry.gauge("ledger.outbox.pending.oldest.age", new AtomicLong(0));
    }

    public void incrementClaimCount(int size) {
        claimCounter.increment(size);
    }

    public void incrementPublishCount(int size) {
        publishCounter.increment(size);
    }

    public void incrementPublishSuccessCount(int size) {
        publishSuccessCounter.increment(size);
        publishSuccessStandaloneCounter.increment(size);
    }

    public void incrementPublishFailureCount() {
        publishFailureCounter.increment();
        publishFailureStandaloneCounter.increment();
    }

    public <T> T recordClaim(java.util.concurrent.Callable<T> callable) {
        try {
            return claimTimer.recordCallable(callable);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to record outbox claim metric", e);
        }
    }

    public void recordPublish(Runnable runnable) {
        publishTimer.record(runnable);
    }

    public void recordFinalize(Runnable runnable) {
        finalizeTimer.record(runnable);
    }

    public void recordBatch(Runnable runnable) {
        batchTimer.record(runnable);
    }

    public void setPendingSize(long count) {
        pendingSizeGauge.set(count);
    }

    public void setProcessingSize(long count) {
        processingSizeGauge.set(count);
    }

    public void setFailedSize(long count) {
        failedSizeGauge.set(count);
    }

    public void setOldestPendingAgeSeconds(long seconds) {
        oldestPendingAgeSecondsGauge.set(seconds);
    }
}
