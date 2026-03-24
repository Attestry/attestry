package io.attestry.kafka.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class ProjectionEventParser {

    private final ObjectMapper objectMapper;

    ProjectionEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ProjectionEventContext parse(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String passportId = ProjectionPayloadReader.text(root, "passportId");
        String eventAction = ProjectionPayloadReader.text(root, "eventAction");
        Instant occurredAt = root.hasNonNull("occurredAt")
            ? Instant.parse(root.get("occurredAt").asText())
            : Instant.now();
        return new ProjectionEventContext(
            root,
            ProjectionAggregateType.from(root.path("aggregateType").asText()),
            passportId,
            ProjectionEventCategory.from(ProjectionPayloadReader.text(root, "eventCategory")),
            ProjectionEventAction.from(eventAction),
            occurredAt,
            safeEventId(ProjectionPayloadReader.text(root, "idempotencyKey"), passportId, eventAction, occurredAt)
        );
    }

    private String safeEventId(String idempotencyKey, String passportId, String eventAction, Instant occurredAt) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKey;
        }
        return "consumer:"
            + (passportId == null ? "UNKNOWN" : passportId)
            + ":"
            + (eventAction == null ? "UNKNOWN" : eventAction)
            + ":"
            + occurredAt.toEpochMilli();
    }
}
