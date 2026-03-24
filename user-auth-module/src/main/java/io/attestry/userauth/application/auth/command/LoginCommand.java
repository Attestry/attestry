package io.attestry.userauth.application.auth.command;

public record LoginCommand(
    String email,
    String password,
    String tenantId
) {
}
