package io.attestry.userauth.application.membership.command;

public record ApplyPermissionTemplateCommand(String templateCode, String reason) {
}
