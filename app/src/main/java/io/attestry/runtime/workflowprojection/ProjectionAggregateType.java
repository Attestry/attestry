package io.attestry.runtime.workflowprojection;

enum ProjectionAggregateType {
    PRODUCT,
    SHIPMENT,
    DISTRIBUTION,
    TRANSFER,
    UNKNOWN;

    static ProjectionAggregateType from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return ProjectionAggregateType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
