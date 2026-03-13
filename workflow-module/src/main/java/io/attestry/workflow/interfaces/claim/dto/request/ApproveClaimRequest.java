package io.attestry.workflow.interfaces.claim.dto.request;

import java.time.Instant;

public record ApproveClaimRequest(Instant manufacturedAt, String productionBatch, String factoryCode) {
}
