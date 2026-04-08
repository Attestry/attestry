package io.attestry.runtime.workflowprojection;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class WorkflowProjectionMetrics {

    private final Counter dlqCounter;
    private final Counter dlqPublishSuccessCounter;
    private final Counter dlqPublishFailureCounter;
    private final Counter consumeSuccessCounter;
    private final Counter consumeIgnoredCounter;
    private final Counter consumeFailureCounter;
    private final Timer projectionRefreshTimer;
    private final Timer projectionLagTimer;
    private final Timer dlqPublishTimer;
    private final Clock clock;

    WorkflowProjectionMetrics(MeterRegistry meterRegistry) {
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
    }

    void recordSuccess() {
        consumeSuccessCounter.increment();
    }

    void recordIgnored() {
        consumeIgnoredCounter.increment();
    }

    void recordFailure() {
        dlqCounter.increment();
        consumeFailureCounter.increment();
    }

    void recordLag(Instant occurredAt) {
        projectionLagTimer.record(Duration.between(occurredAt, Instant.now(clock)).abs());
    }

    void recordDlqPublish(Duration duration, boolean success) {
        dlqPublishTimer.record(duration.abs());
        if (success) {
            dlqPublishSuccessCounter.increment();
            return;
        }
        dlqPublishFailureCounter.increment();
    }

    Timer projectionRefreshTimer() {
        return projectionRefreshTimer;
    }

    Clock clock() {
        return clock;
    }
}
