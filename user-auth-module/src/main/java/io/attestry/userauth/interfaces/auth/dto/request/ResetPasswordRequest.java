package io.attestry.userauth.interfaces.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z]).{8,}$",
        message = "Password must be at least 8 characters and contain at least one uppercase letter"
    )
    String newPassword
) {
}
