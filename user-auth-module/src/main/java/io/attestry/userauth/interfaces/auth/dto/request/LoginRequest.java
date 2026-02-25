package io.attestry.userauth.interfaces.auth.dto.request;

public record LoginRequest(String email, String password, String tenantId, String groupId) {
}