package io.attestry.workflow.interfaces.servicerequest.dto.request;

public record AcceptServiceRequestRequest(
    String serviceType,
    String description
) {
}
