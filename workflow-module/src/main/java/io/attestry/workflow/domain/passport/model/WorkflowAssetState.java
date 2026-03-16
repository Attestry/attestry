package io.attestry.workflow.domain.passport.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;

public enum WorkflowAssetState {
    ACTIVE,
    VOIDED,
    RETIRED;

    public static WorkflowAssetState from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return WorkflowAssetState.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.INVALID_STATE,
                "Unknown passport asset state: " + value
            );
        }
    }
}
