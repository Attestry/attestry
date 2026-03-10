package io.attestry.userauth.interfaces.membership.dto.request;

import io.attestry.userauth.domain.membership.model.MembershipRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        String email,

        @NotBlank(message = "role is required")
        MembershipRole role) {
}
