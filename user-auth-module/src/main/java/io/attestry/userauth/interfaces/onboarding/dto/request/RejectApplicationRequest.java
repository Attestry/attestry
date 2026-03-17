package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectApplicationRequest(
        @NotBlank(message = "반려 사유는 필수입니다")
        @Size(max = 1000, message = "반려 사유는 1000자 이하로 입력해주세요.")
        String reason) {
}
