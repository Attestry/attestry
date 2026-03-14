package io.attestry.product.application.dto.command;

import java.util.Set;

public record ProductActor(
    String userId,
    String tenantId,
    Set<String> scopes,
    boolean platformAdmin
) {
}
