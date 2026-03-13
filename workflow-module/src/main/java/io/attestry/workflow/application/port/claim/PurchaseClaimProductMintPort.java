package io.attestry.workflow.application.port.claim;

import java.time.Instant;
import java.util.Set;

public interface PurchaseClaimProductMintPort {

    MintResult mint(MintRequest request);

    record MintRequest(
        String actorUserId,
        String actorTenantId,
        Set<String> actorScopes,
        boolean platformAdmin,
        String tenantId,
        String serialNumber,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode
    ) {
    }

    record MintResult(
        String assetId,
        String passportId,
        String qrPublicCode,
        String outboxEventId,
        String ledgerEventCategory,
        String ledgerEventAction
    ) {
    }
}
