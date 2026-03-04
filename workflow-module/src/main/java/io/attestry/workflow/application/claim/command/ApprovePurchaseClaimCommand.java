package io.attestry.workflow.application.claim.command;

import java.time.Instant;

public record ApprovePurchaseClaimCommand(
    Instant manufacturedAt,
    String productionBatch,
    String factoryCode
) {
}
