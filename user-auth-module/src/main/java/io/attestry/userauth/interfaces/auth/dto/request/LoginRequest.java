package io.attestry.userauth.interfaces.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "비밀번호는 8자 이상이며 영문 대문자를 1자 이상 포함해야 합니다"
    )
    String password,

    String tenantId
) {
}
