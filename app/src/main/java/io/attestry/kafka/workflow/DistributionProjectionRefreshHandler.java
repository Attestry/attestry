package io.attestry.kafka.workflow;

import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort;
import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort.DistributionPayload;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DistributionProjectionRefreshHandler implements ProjectionRefreshHandler {

    private static final Logger log = LoggerFactory.getLogger(DistributionProjectionRefreshHandler.class);

    private final ProductDistributionProjectionWritePort distributionProjectionWriter;
    private final Timer projectionRefreshTimer;
    private final ProjectionPayloadMapper payloadMapper;

    DistributionProjectionRefreshHandler(
        ProductDistributionProjectionWritePort distributionProjectionWriter,
        Timer projectionRefreshTimer
    ) {
        this.distributionProjectionWriter = distributionProjectionWriter;
        this.projectionRefreshTimer = projectionRefreshTimer;
        this.payloadMapper = new ProjectionPayloadMapper();
    }

    @Override
    public boolean refresh(ProjectionEventContext context) {
        if (context.aggregateType() != ProjectionAggregateType.DISTRIBUTION ||
            context.eventCategory() != ProjectionEventCategory.DISTRIBUTION) {
            return false;
        }

        DistributionPayload distributionPayload = payloadMapper.toDistributionPayload(context);

        projectionRefreshTimer.record(() ->
            distributionProjectionWriter.refreshDistributionProjection(
                distributionPayload, context.sourceEventId(), null, context.occurredAt()
            )
        );
        log.debug("distribution projection refreshed: passportId={}, eventAction={}",
            context.passportId(), context.eventAction());
        return true;
    }
}
