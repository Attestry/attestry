package io.attestry.userauth.interfaces.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    String currentPassword,

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "비밀번호는 8자 이상이며 영문 대문자를 1자 이상 포함해야 합니다"
    )
    String newPassword
) {
}
