package io.attestry.workflow.domain.servicerequest.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.Set;

public final class ServiceTypes {

    public static final Set<String> ALLOWED = Set.of(
        "REPAIR",
        "CLEANING",
        "INSPECTION",
        "MAINTENANCE",
        "AUTHENTICATION"
    );

    private ServiceTypes() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "serviceType is required");
        }
        String normalized = raw.trim().toUpperCase();
        if (!ALLOWED.contains(normalized)) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Invalid serviceType");
        }
        return normalized;
    }
}
