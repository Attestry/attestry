package io.attestry.userauth.application.auth.command;

import java.time.Instant;

public record AuthTokenResult(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    String userId,
    String tenantId
) {
}
