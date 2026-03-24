package io.attestry.kafka.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort.EvidencePayload;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class ProjectionPayloadReader {

    private ProjectionPayloadReader() {
    }

    static String text(JsonNode root, String fieldName) {
        return root.hasNonNull(fieldName) ? root.get(fieldName).asText() : null;
    }

    static Instant parseInstant(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    static List<EvidencePayload> parseEvidences(JsonNode evidenceDetailsNode) {
        if (evidenceDetailsNode == null || evidenceDetailsNode.isMissingNode() || !evidenceDetailsNode.isArray()) {
            return List.of();
        }
        List<EvidencePayload> result = new ArrayList<>();
        for (JsonNode node : evidenceDetailsNode) {
            result.add(new EvidencePayload(
                text(node, "evidenceId"),
                text(node, "originalFileName"),
                text(node, "contentType"),
                node.path("sizeBytes").asLong(0),
                text(node, "objectKey")
            ));
        }
        return result;
    }
}
