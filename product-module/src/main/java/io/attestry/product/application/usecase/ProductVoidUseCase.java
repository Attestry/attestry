package io.attestry.product.application.usecase;

import io.attestry.userauth.application.dto.command.ActorContext;

public interface ProductVoidUseCase {

    VoidResult voidAsset(ActorContext actor, VoidCommand command);

    record VoidCommand(String tenantId, String groupId, String passportId, String reason, String note) {
    }

    record VoidResult(String assetId, String assetState, String outboxEventId) {
    }
}
