package io.attestry.runtime.workflowprojection;

import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort;
import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort.ShipmentPayload;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class ShipmentProjectionRefreshHandler implements ProjectionRefreshHandler {

    private static final Logger log = LoggerFactory.getLogger(ShipmentProjectionRefreshHandler.class);

    private final ProductShipmentProjectionWritePort shipmentProjectionWriter;
    private final Timer projectionRefreshTimer;
    private final ProjectionPayloadMapper payloadMapper;

    ShipmentProjectionRefreshHandler(
        ProductShipmentProjectionWritePort shipmentProjectionWriter,
        WorkflowProjectionMetrics metrics,
        ProjectionPayloadMapper payloadMapper
    ) {
        this.shipmentProjectionWriter = shipmentProjectionWriter;
        this.projectionRefreshTimer = metrics.projectionRefreshTimer();
        this.payloadMapper = payloadMapper;
    }

    @Override
    public boolean refresh(ProjectionEventContext context) {
        if (context.aggregateType() != ProjectionAggregateType.SHIPMENT ||
            context.eventCategory() != ProjectionEventCategory.SHIPMENT) {
            return false;
        }

        ShipmentPayload shipmentPayload = payloadMapper.toShipmentPayload(context);

        projectionRefreshTimer.record(() ->
            shipmentProjectionWriter.refreshShipmentProjection(
                shipmentPayload, context.sourceEventId(), null, context.occurredAt()
            )
        );
        log.debug("shipment projection refreshed: passportId={}, eventAction={}",
            context.passportId(), context.eventAction());
        return true;
    }
}
