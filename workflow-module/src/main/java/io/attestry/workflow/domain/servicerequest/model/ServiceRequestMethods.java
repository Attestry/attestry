package io.attestry.workflow.domain.servicerequest.model;

import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.Set;

public final class ServiceRequestMethods {

    public static final String VISIT = "VISIT";
    public static final String ONLINE = "ONLINE";

    private static final Set<String> ALLOWED = Set.of(VISIT, ONLINE);

    private ServiceRequestMethods() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "serviceRequestMethod is required");
        }
        String normalized = raw.trim().toUpperCase();
        if (!ALLOWED.contains(normalized)) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Invalid serviceRequestMethod");
        }
        return normalized;
    }
}
