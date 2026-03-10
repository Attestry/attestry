package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.OwnerResult;
import java.time.Instant;

public record OwnerResponse(
    String passportId,
    String ownerId,
    Instant updatedAt
) {
    public static OwnerResponse from(OwnerResult result) {
        return new OwnerResponse(result.passportId(), result.ownerId(), result.updatedAt());
    }
}
