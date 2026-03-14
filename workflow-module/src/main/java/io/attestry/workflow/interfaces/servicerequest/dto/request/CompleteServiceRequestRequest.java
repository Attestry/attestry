package io.attestry.workflow.interfaces.servicerequest.dto.request;

public record CompleteServiceRequestRequest(
    String serviceType,
    String afterEvidenceGroupId,
    String serviceResult,
    String completionMemo
) {
}
