package io.attestry.runtime.workflowprojection;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

record ProjectionEventContext(
    JsonNode root,
    ProjectionAggregateType aggregateType,
    String passportId,
    ProjectionEventCategory eventCategory,
    ProjectionEventAction eventAction,
    Instant occurredAt,
    String sourceEventId
) {
}
