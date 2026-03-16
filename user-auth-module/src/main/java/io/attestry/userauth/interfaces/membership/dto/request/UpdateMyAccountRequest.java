package io.attestry.userauth.interfaces.membership.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateMyAccountRequest(
    @Pattern(
        regexp = "^010-(?!0000)\\d{4}-\\d{4}$",
        message = "휴대폰 번호는 010-0000-0000 형식이어야 하며, 가운데 4자리는 0000일 수 없습니다"
    )
    String phone,
    String currentPassword,
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "비밀번호는 8자 이상이며 영문 대문자를 1자 이상 포함해야 합니다"
    )
    String newPassword
) {
}
