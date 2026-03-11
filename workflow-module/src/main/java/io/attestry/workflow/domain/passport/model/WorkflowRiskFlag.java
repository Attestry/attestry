package io.attestry.workflow.domain.passport.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;

public enum WorkflowRiskFlag {
    NONE,
    FLAGGED;

    public static WorkflowRiskFlag from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return WorkflowRiskFlag.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Unknown passport risk flag: " + value
            );
        }
    }
}
