package io.attestry.workflow.domain;

public class WorkflowDomainException extends RuntimeException {

    private final WorkflowErrorCode errorCode;

    public WorkflowDomainException(WorkflowErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WorkflowErrorCode getErrorCode() {
        return errorCode;
    }
}
