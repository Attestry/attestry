package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.Size;

public record CompleteServiceRequestRequest(
    String serviceType,
    String afterEvidenceGroupId,
    @Size(max = 2000, message = "완료 내용은 2000자 이하로 입력해주세요.")
    String serviceResult,
    @Size(max = 2000, message = "추가 메모는 2000자 이하로 입력해주세요.")
    String completionMemo
) {
}
