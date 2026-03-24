package io.attestry.product.interfaces.http.query.dto.response;

import io.attestry.product.application.query.view.OwnerView;
import java.time.Instant;

public record OwnerResponse(
    String passportId,
    String ownerId,
    Instant updatedAt
) {
    public static OwnerResponse from(OwnerView view) {
        return new OwnerResponse(view.passportId(), view.ownerId(), view.updatedAt());
    }
}
