package io.attestry.userauth.application.membership.command;

public record RevokePermissionTemplateCommand(String templateCode, String reason) {
}
