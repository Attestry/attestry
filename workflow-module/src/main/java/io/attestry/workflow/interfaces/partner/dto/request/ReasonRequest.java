package io.attestry.workflow.interfaces.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonRequest(
    @NotBlank(message = "사유는 필수입니다.")
    @Size(max = 1000, message = "사유는 1000자 이하로 입력해주세요.")
    String reason
) {
}
