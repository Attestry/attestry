package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.Size;

public record CancelServiceRequestRequest(
    @Size(max = 1000, message = "취소 사유는 1000자 이하로 입력해주세요.")
    String cancelReason
) {
}
