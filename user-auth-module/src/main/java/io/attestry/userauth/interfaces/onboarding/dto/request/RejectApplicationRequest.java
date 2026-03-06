package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectApplicationRequest(
        @NotBlank(message = "반려 사유는 필수입니다") String reason) {
}
