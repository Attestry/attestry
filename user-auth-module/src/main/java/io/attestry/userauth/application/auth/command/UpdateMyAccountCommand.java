package io.attestry.userauth.application.auth.command;

public record UpdateMyAccountCommand(
    String phone,
    String currentPassword,
    String newPassword
) {

    public boolean hasPasswordChangeRequest() {
        return newPassword != null && !newPassword.isBlank();
    }
}
