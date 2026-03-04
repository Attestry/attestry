package io.attestry.product.domain.event;

import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.model.RiskFlag;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record LedgerEventEnvelope(
    String passportId,
    String eventCategory,
    String eventAction,
    String actorId,
    Instant occurredAt,
    Map<String, Object> payload,
    String idempotencyKey
) {

    //TODO("도메인 이벤트로 리팩토링")
    public static LedgerEventEnvelope minted(ProductPassport passport, Instant occurredAt) {
        ProductAsset asset = passport.getAsset();
        return new LedgerEventEnvelope(
            passport.getPassportId(),
            "GENESIS",
            "MINTED",
            passport.getGroupId(),
            occurredAt,
            Map.of(
                "actorRole", "BRAND",
                "assetId", asset.getAssetId(),
                "serialNumber", asset.getSerialNumber(),
                "modelId", asset.getModelId() == null ? "" : asset.getModelId(),
                "modelName", asset.getModelName(),
                "manufacturedAt", asset.getManufacturedAt().toString(),
                "qrPublicCode", passport.getQrPublicCode(),
                "productionBatch", asset.getProductionBatch() == null ? "" : asset.getProductionBatch(),
                "factoryCode", asset.getFactoryCode() == null ? "" : asset.getFactoryCode(),
                "componentRootHash", asset.getComponentRootHash() == null ? "" : asset.getComponentRootHash()
            ),
            "mint-" + passport.getPassportId()
        );
    }

    public static LedgerEventEnvelope voided(ProductPassport passport, String actorId, Instant occurredAt) {
        return new LedgerEventEnvelope(
            passport.getPassportId(),
            "LIFECYCLE",
            "VOIDED",
            actorId,
            occurredAt,
            Map.of(
                "assetId", passport.getAsset().getAssetId(),
                "reason", passport.getAsset().getVoidedReason().name(),
                "note", passport.getAsset().getVoidedNote() == null ? "" : passport.getAsset().getVoidedNote()
            ),
            "void-" + passport.getPassportId()
        );
    }

    public static LedgerEventEnvelope riskFlagged(ProductPassport passport, String actorId, Instant occurredAt) {
        ProductAsset asset = passport.getAsset();
        String action = asset.getRiskFlag() == RiskFlag.STOLEN ? "STOLEN_FLAGGED" : "LOST_FLAGGED";

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("assetId", asset.getAssetId());
        payloadMap.put("riskFlag", asset.getRiskFlag().name());
        payloadMap.put("reportedBy", asset.getRiskReportedBy() == null ? "" : asset.getRiskReportedBy());
        if (asset.getPoliceReportNo() != null) {
            payloadMap.put("policeReportNo", asset.getPoliceReportNo());
        }

        return new LedgerEventEnvelope(
            passport.getPassportId(),
            "RISK",
            action,
            actorId,
            occurredAt,
            Map.copyOf(payloadMap),
            "risk-flag-" + passport.getPassportId() + "-" + occurredAt.toEpochMilli()
        );
    }

    public static LedgerEventEnvelope riskCleared(ProductPassport passport, String actorId, Instant occurredAt) {
        return new LedgerEventEnvelope(
            passport.getPassportId(),
            "RISK",
            "RISK_CLEARED",
            actorId,
            occurredAt,
            Map.of("assetId", passport.getAsset().getAssetId()),
            "risk-clear-" + passport.getPassportId() + "-" + occurredAt.toEpochMilli()
        );
    }
}
