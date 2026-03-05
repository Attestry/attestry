package io.attestry.workflow.domain;

public final class WorkflowValidation {

    private WorkflowValidation() {}

    public static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, field + " is required");
        }
    }

    public static <T> void requireNonNull(T value, String field) {
        if (value == null) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, field + " is required");
        }
    }
}
