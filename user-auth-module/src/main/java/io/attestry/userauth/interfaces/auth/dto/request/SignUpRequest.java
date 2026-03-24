package io.attestry.userauth.interfaces.auth.dto.request;

import io.attestry.userauth.domain.auth.model.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Pattern(regexp = Email.VALIDATION_PATTERN, message = "올바른 이메일 형식을 입력해주세요.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "비밀번호는 8자 이상이며 영문 대문자를 1자 이상 포함해야 합니다"
    )
    String password,

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(
        regexp = "^010-(?!0000)\\d{4}-\\d{4}$",
        message = "휴대폰 번호는 010-0000-0000 형식이어야 하며, 가운데 4자리는 0000일 수 없습니다"
    )
    String phone
) {
}
