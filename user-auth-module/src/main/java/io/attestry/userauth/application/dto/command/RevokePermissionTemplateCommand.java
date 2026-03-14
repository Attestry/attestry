package io.attestry.userauth.application.dto.command;

public record RevokePermissionTemplateCommand(String templateCode, String reason) {
}
