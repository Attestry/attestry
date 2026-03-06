package io.attestry.product.application.usecase;

import io.attestry.userauth.application.dto.command.ActorContext;
import java.time.Instant;

public interface ProductMintUseCase {

    MintedProductResult mint(ActorContext actor, MintProductCommand command);

    record MintProductCommand(
        String tenantId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash
    ) {
    }

    record MintedProductResult(
        String assetId,
        String passportId,
        String qrPublicCode,
        String outboxEventId,
        String ledgerEventCategory,
        String ledgerEventAction
    ) {
    }
}
