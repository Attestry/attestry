package io.attestry.userauth.interfaces.membership.dto.request;

import io.attestry.userauth.domain.membership.model.MembershipStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateMembershipStatusRequest(
        @NotBlank
        MembershipStatus status
) {
}
