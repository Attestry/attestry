package io.attestry.userauth.interfaces.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TenantSwitchRequest(
    @NotBlank(message = "멤버십 ID는 필수입니다")
    String membershipId
) {
}
