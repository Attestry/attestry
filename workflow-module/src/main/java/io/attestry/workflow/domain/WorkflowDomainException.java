package io.attestry.workflow.domain;

import io.attestry.commonlib.domain.exception.DomainException;

public class WorkflowDomainException extends DomainException {

    public WorkflowDomainException(WorkflowErrorCode errorCode) {
        super(errorCode);
    }

    public WorkflowDomainException(WorkflowErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public WorkflowDomainException(WorkflowErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
