package io.attestry.kafka.workflow;

enum ProjectionEventCategory {
    GENESIS,
    LIFECYCLE,
    RISK,
    SHIPMENT,
    DISTRIBUTION,
    OWNERSHIP,
    UNKNOWN;

    static ProjectionEventCategory from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return ProjectionEventCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
