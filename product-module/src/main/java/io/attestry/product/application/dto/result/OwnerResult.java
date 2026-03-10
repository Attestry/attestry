package io.attestry.product.application.dto.result;

import java.time.Instant;

public record OwnerResult(String passportId, String ownerId, Instant updatedAt) {
}
