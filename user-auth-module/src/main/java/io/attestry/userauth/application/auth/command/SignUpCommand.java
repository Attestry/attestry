package io.attestry.userauth.application.auth.command;

public record SignUpCommand(
    String email,
    String password,
    String phone
) {
}
