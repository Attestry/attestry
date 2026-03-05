package io.attestry.userauth.application.dto.command;

public record UpdateMyAccountCommand(
    String phone,
    String currentPassword,
    String newPassword
) {
}
