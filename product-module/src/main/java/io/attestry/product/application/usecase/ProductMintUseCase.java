package io.attestry.product.application.usecase;

import io.attestry.userauth.application.dto.command.ActorContext;
import java.time.Instant;
import java.util.List;

public interface ProductMintUseCase {

    MintedProductResult mint(ActorContext actor, MintProductCommand command);

    BatchMintResult batchMint(ActorContext actor, String tenantId, List<MintProductCommand> commands);

    record MintProductCommand(
        String tenantId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String componentRootHash,
        String actorRoleOverride
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

    record BatchMintResult(
        int totalRequested,
        int totalMinted,
        int totalFailed,
        List<BatchMintError> errors
    ) {
    }

    record BatchMintError(
        int row,
        String serialNumber,
        String reason
    ) {
    }
}
