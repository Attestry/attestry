package io.attestry.userauth.application.dto.command;

public record SignUpCommand(
    String email,
    String password,
    String phone
) {
}
