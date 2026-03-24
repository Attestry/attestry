package io.attestry.product.application.query.view;

import java.time.Instant;

public record OwnerView(String passportId, String ownerId, Instant updatedAt) {
}
