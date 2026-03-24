package io.attestry.userauth.application.auth.command;

public record ResetPasswordCommand(
    String currentPassword,
    String newPassword
) {
}
