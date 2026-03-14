package io.attestry.product.domain.passport.model;

import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.time.Instant;

public class ProductPassport {

    private final String passportId;
    private final String tenantId;
    private final String qrPublicCode;
    private final ProductAsset asset;
    private final Instant createdAt;

    private ProductPassport(
        String passportId,
        String tenantId,
        String qrPublicCode,
        ProductAsset asset,
        Instant createdAt
    ) {
        this.passportId = passportId;
        this.tenantId = tenantId;
        this.qrPublicCode = qrPublicCode;
        this.asset = asset;
        this.createdAt = createdAt;
    }

    // --- Factory ---

    public static ProductPassport mint(
        String passportId,
        String qrPublicCode,
        String assetId,
        MintProductInput input,
        Instant now
    ) {
        if (now == null) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "createdAt is required");
        }
        ProductAsset asset = ProductAsset.create(assetId, input, now);
        return new ProductPassport(
            requireText(passportId, "passportId"),
            input.tenantId(),
            requireText(qrPublicCode, "qrPublicCode"),
            asset,
            now
        );
    }

    public static ProductPassport reconstitute(
        String passportId,
        String tenantId,
        String qrPublicCode,
        ProductAsset asset,
        Instant createdAt
    ) {
        return new ProductPassport(passportId, tenantId, qrPublicCode, asset, createdAt);
    }

    // --- Delegate behaviors to asset ---

    public void voidAsset(VoidReason reason, String note, Instant now) {
        asset.voidAsset(reason, note, now);
    }

    public void flagStolen(String reportedBy, String policeReportNo, Instant now) {
        asset.flagStolen(reportedBy, policeReportNo, now);
    }

    public void flagLost(String reportedBy, Instant now) {
        asset.flagLost(reportedBy, now);
    }

    public void clearRisk() {
        asset.clearRisk();
    }

    // --- Getters ---

    public String getPassportId() { return passportId; }
    public String getTenantId() { return tenantId; }
    public String getQrPublicCode() { return qrPublicCode; }
    public ProductAsset getAsset() { return asset; }
    public Instant getCreatedAt() { return createdAt; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }
}
