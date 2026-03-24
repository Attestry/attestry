package io.attestry.userauth.interfaces.auth.dto.request;

import io.attestry.userauth.domain.auth.model.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendSignUpEmailVerificationRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Pattern(regexp = Email.VALIDATION_PATTERN, message = "올바른 이메일 형식을 입력해주세요.")
    String email
) {
}
