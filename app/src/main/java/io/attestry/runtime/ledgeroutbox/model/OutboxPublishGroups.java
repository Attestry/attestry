package io.attestry.runtime.ledgeroutbox.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record OutboxPublishGroups(Map<String, List<OutboxEventRecord>> byAggregateId) {

    public static OutboxPublishGroups from(List<OutboxEventRecord> events) {
        Map<String, List<OutboxEventRecord>> grouped = new LinkedHashMap<>();
        for (OutboxEventRecord event : events) {
            grouped.computeIfAbsent(event.aggregateId(), ignored -> new ArrayList<>()).add(event);
        }
        return new OutboxPublishGroups(grouped);
    }
}
