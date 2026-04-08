package io.attestry.runtime.workflowprojection;

enum ProjectionEventAction {
    MINTED,
    VOIDED,
    RETIRED,
    CLAIMED,
    UNKNOWN;

    static ProjectionEventAction from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return ProjectionEventAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
