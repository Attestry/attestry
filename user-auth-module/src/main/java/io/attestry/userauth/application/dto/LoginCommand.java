package io.attestry.userauth.application.dto;

public record LoginCommand(
    String email,
    String password,
    String tenantId,
    String groupId
) {
}
