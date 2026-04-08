package io.attestry.runtime.workflowprojection;

import com.fasterxml.jackson.databind.JsonNode;
import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort.DistributionPayload;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort.RetailAccessPayload;
import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort.ShipmentPayload;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort.ProductStatePayload;
import org.springframework.stereotype.Component;

@Component
final class ProjectionPayloadMapper {

    ProductStatePayload toProductStatePayload(ProjectionEventContext context) {
        JsonNode payloadNode = context.root().path("payload");
        return new ProductStatePayload(
            context.passportId(),
            ProjectionPayloadReader.text(payloadNode, "tenantId"),
            ProjectionPayloadReader.text(payloadNode, "assetId"),
            ProjectionPayloadReader.text(payloadNode, "assetState"),
            ProjectionPayloadReader.text(payloadNode, "riskFlagProjection"),
            ProjectionPayloadReader.text(payloadNode, "serialNumber"),
            ProjectionPayloadReader.text(payloadNode, "modelId"),
            ProjectionPayloadReader.text(payloadNode, "modelName"),
            ProjectionPayloadReader.text(payloadNode, "productionBatch"),
            ProjectionPayloadReader.text(payloadNode, "factoryCode"),
            ProjectionPayloadReader.text(payloadNode, "manufacturedAt")
        );
    }

    ShipmentPayload toShipmentPayload(ProjectionEventContext context) {
        JsonNode payloadNode = context.root().path("payload");
        return new ShipmentPayload(
            context.passportId(),
            ProjectionPayloadReader.text(payloadNode, "shipmentId"),
            ProjectionPayloadReader.text(payloadNode, "status"),
            payloadNode.path("shipmentRound").asInt(0),
            ProjectionPayloadReader.parseInstant(payloadNode, "releasedAt"),
            ProjectionPayloadReader.text(payloadNode, "releasedByUserDisplay"),
            ProjectionPayloadReader.parseInstant(payloadNode, "returnedAt"),
            ProjectionPayloadReader.text(payloadNode, "returnedByUserDisplay"),
            ProjectionPayloadReader.parseEvidences(payloadNode.path("evidenceDetails"))
        );
    }

    DistributionPayload toDistributionPayload(ProjectionEventContext context) {
        JsonNode payloadNode = context.root().path("payload");
        return new DistributionPayload(
            context.passportId(),
            ProjectionPayloadReader.text(payloadNode, "distributionId"),
            ProjectionPayloadReader.text(payloadNode, "targetTenantId"),
            ProjectionPayloadReader.text(payloadNode, "targetTenantName"),
            ProjectionPayloadReader.text(payloadNode, "targetTenantType"),
            ProjectionPayloadReader.text(payloadNode, "partnerLinkId"),
            ProjectionPayloadReader.text(payloadNode, "status"),
            ProjectionPayloadReader.parseInstant(payloadNode, "distributedAt")
        );
    }

    RetailAccessPayload toRetailAccessPayload(ProjectionEventContext context) {
        JsonNode payloadNode = context.root().path("payload");
        String transferId = ProjectionPayloadReader.text(payloadNode, "transferId");
        if (transferId == null || transferId.isBlank()) {
            return null;
        }
        return new RetailAccessPayload(
            context.passportId(),
            transferId,
            ProjectionPayloadReader.text(payloadNode, "tenantId"),
            ProjectionPayloadReader.parseInstant(payloadNode, "completedAt")
        );
    }
}
