package io.attestry.userauth.application.dto.command;

public record ApplyPermissionTemplateCommand(String templateCode, String reason) {
}
