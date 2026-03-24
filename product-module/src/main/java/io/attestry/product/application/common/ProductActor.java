package io.attestry.product.application.common;

import java.util.Set;

public record ProductActor(
    String userId,
    String tenantId,
    Set<String> scopes,
    boolean platformAdmin
) {
}
