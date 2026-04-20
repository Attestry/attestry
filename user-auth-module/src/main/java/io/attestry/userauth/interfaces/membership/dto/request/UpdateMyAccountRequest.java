package io.attestry.userauth.interfaces.membership.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateMyAccountRequest(
    @Pattern(
        regexp = "^010-(?!0000)\\d{4}-\\d{4}$",
        message = "Phone number must be in 010-XXXX-XXXX format, middle digits cannot be 0000"
    )
    String phone,
    String currentPassword,
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "Password must be at least 8 characters and contain at least one uppercase letter"
    )
    String newPassword
) {
}
