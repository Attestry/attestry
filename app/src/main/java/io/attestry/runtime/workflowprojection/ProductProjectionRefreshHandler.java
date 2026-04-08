package io.attestry.runtime.workflowprojection;

import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort.ProductStatePayload;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class ProductProjectionRefreshHandler implements ProjectionRefreshHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductProjectionRefreshHandler.class);

    private final WorkflowPassportProjectionWritePort projectionWriter;
    private final Timer projectionRefreshTimer;
    private final ProjectionPayloadMapper payloadMapper;

    ProductProjectionRefreshHandler(
        WorkflowPassportProjectionWritePort projectionWriter,
        WorkflowProjectionMetrics metrics,
        ProjectionPayloadMapper payloadMapper
    ) {
        this.projectionWriter = projectionWriter;
        this.projectionRefreshTimer = metrics.projectionRefreshTimer();
        this.payloadMapper = payloadMapper;
    }

    @Override
    public boolean refresh(ProjectionEventContext context) {
        if (context.aggregateType() != ProjectionAggregateType.PRODUCT ||
            !ProductProjectionRefreshPolicy.shouldRefreshStateAndCatalog(context.eventCategory(), context.eventAction())) {
            return false;
        }

        ProductStatePayload statePayload = payloadMapper.toProductStatePayload(context);

        projectionRefreshTimer.record(() ->
            projectionWriter.refreshStateAndCatalog(
                statePayload, context.sourceEventId(), null, context.occurredAt()
            )
        );
        log.debug(
            "workflow product projection refreshed: passportId={}, eventCategory={}, eventAction={}",
            context.passportId(),
            context.eventCategory(),
            context.eventAction()
        );
        return true;
    }
}
