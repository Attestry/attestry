package io.attestry.product.application.event;

import io.attestry.commonlib.outbox.OutboxEventEnvelope;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.model.RiskFlag;
import java.time.Instant;

public final class ProductLedgerEvents {

    private ProductLedgerEvents() {
    }

    public static OutboxEventEnvelope minted(ProductPassport passport, String actorRole, String actorId, Instant occurredAt) {
        return new OutboxEventEnvelope(
            "PRODUCT",
            passport.getPassportId(),
            "GENESIS",
            "MINTED",
            actorRole,
            actorId,
            occurredAt,
            ProductEventPayloads.mintedPayload(passport).toMap(),
            "mint-" + passport.getPassportId()
        );
    }

    public static OutboxEventEnvelope voided(ProductPassport passport, String actorId, Instant occurredAt) {
        return new OutboxEventEnvelope(
            "PRODUCT",
            passport.getPassportId(),
            "LIFECYCLE",
            "VOIDED",
            "BRAND",
            actorId,
            occurredAt,
            ProductEventPayloads.voidedPayload(passport).toMap(),
            "void-" + passport.getPassportId()
        );
    }

    public static OutboxEventEnvelope retired(ProductPassport passport, String actorId, Instant occurredAt) {
        return new OutboxEventEnvelope(
            "PRODUCT",
            passport.getPassportId(),
            "LIFECYCLE",
            "RETIRED",
            "OWNER",
            actorId,
            occurredAt,
            ProductEventPayloads.retiredPayload(passport).toMap(),
            "retire-" + passport.getPassportId()
        );
    }

    public static OutboxEventEnvelope riskFlagged(ProductPassport passport, String actorId, Instant occurredAt) {
        String action = passport.getAsset().getRiskFlag() == RiskFlag.STOLEN ? "STOLEN_FLAGGED" : "LOST_FLAGGED";
        return new OutboxEventEnvelope(
            "PRODUCT",
            passport.getPassportId(),
            "RISK",
            action,
            "OWNER",
            actorId,
            occurredAt,
            ProductEventPayloads.riskFlaggedPayload(passport).toMap(),
            "risk-flag-" + passport.getPassportId() + "-" + occurredAt.toEpochMilli()
        );
    }

    public static OutboxEventEnvelope riskCleared(ProductPassport passport, String actorId, Instant occurredAt, RiskFlag clearedRiskFlag) {
        return new OutboxEventEnvelope(
            "PRODUCT",
            passport.getPassportId(),
            "RISK",
            "RISK_CLEARED",
            "OWNER",
            actorId,
            occurredAt,
            ProductEventPayloads.riskClearedPayload(passport, clearedRiskFlag).toMap(),
            "risk-clear-" + passport.getPassportId() + "-" + occurredAt.toEpochMilli()
        );
    }
}
