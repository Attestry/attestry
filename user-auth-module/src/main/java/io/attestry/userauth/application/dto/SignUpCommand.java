package io.attestry.userauth.application.dto;

public record SignUpCommand(
    String email,
    String password,
    String phone
) {
}
