package io.attestry.runtime.workflowprojection;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowProductProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(WorkflowProductProjectionConsumer.class);

    private final ProjectionEventParser eventParser;
    private final WorkflowProjectionDlqPublisher dlqPublisher;
    private final WorkflowProjectionMetrics metrics;
    private final List<ProjectionRefreshHandler> refreshHandlers;

    public WorkflowProductProjectionConsumer(
        ProjectionEventParser eventParser,
        WorkflowProjectionDlqPublisher dlqPublisher,
        WorkflowProjectionMetrics metrics,
        List<ProjectionRefreshHandler> refreshHandlers
    ) {
        this.eventParser = eventParser;
        this.dlqPublisher = dlqPublisher;
        this.metrics = metrics;
        this.refreshHandlers = refreshHandlers;
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
                metrics.recordIgnored();
                return;
            }

            metrics.recordLag(context.occurredAt());

            if (refreshHandlers.stream().anyMatch(handler -> handler.refresh(context))) {
                metrics.recordSuccess();
                return;
            }

            metrics.recordIgnored();
        } catch (Exception ex) {
            metrics.recordFailure();
            dlqPublisher.publish(payload, ex);
            log.warn("workflow product projection DLQ: errorType={}, errorMessage={}",
                ex.getClass().getName(), ex.getMessage(), ex);
        }
    }
}
