package io.attestry.userauth.application.dto.command;

public record LoginCommand(
    String email,
    String password,
    String tenantId
) {
}
