package io.attestry.userauth.application.dto.result;

import java.time.Instant;

public record AuthTokenResult(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    String userId,
    String tenantId,
    String groupId
) {
}
