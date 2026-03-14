package io.attestry.userauth.interfaces.membership.dto.request;

public record UpdateMyAccountRequest(
    String phone,
    String currentPassword,
    String newPassword
) {
}
