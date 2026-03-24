package io.attestry.kafka.workflow;

import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort.RetailAccessPayload;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RetailAccessProjectionRefreshHandler implements ProjectionRefreshHandler {

    private static final Logger log = LoggerFactory.getLogger(RetailAccessProjectionRefreshHandler.class);

    private final ProductRetailAccessProjectionWritePort retailAccessProjectionWriter;
    private final Timer projectionRefreshTimer;
    private final ProjectionPayloadMapper payloadMapper;

    RetailAccessProjectionRefreshHandler(
        ProductRetailAccessProjectionWritePort retailAccessProjectionWriter,
        Timer projectionRefreshTimer
    ) {
        this.retailAccessProjectionWriter = retailAccessProjectionWriter;
        this.projectionRefreshTimer = projectionRefreshTimer;
        this.payloadMapper = new ProjectionPayloadMapper();
    }

    @Override
    public boolean refresh(ProjectionEventContext context) {
        if (context.aggregateType() != ProjectionAggregateType.TRANSFER ||
            context.eventCategory() != ProjectionEventCategory.OWNERSHIP ||
            context.eventAction() != ProjectionEventAction.CLAIMED) {
            return false;
        }

        RetailAccessPayload retailPayload = payloadMapper.toRetailAccessPayload(context);
        if (retailPayload == null) {
            return false;
        }

        projectionRefreshTimer.record(() ->
            retailAccessProjectionWriter.refreshB2cTransferAccess(
                retailPayload, context.sourceEventId(), context.occurredAt()
            )
        );
        log.debug("retail access projection refreshed: passportId={}, transferId={}",
            context.passportId(), retailPayload.transferId());
        return true;
    }
}
