package io.attestry.userauth.interfaces.membership.dto.request;

import io.attestry.userauth.domain.membership.model.MembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required")
        MembershipRole role) {
}
