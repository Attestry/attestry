package io.attestry.userauth.interfaces.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        java.time.Instant expiresAt,
        String userId,
        String tenantId,
        String groupId
) {
}