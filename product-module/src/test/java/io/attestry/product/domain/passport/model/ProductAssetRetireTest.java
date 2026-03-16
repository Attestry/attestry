package io.attestry.product.domain.passport.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductAssetRetireTest {

    @Test
    void retire_marksAssetStateRetiredAndStoresTimestamp() {
        Instant now = Instant.parse("2026-03-16T00:00:00Z");
        ProductAsset asset = ProductAsset.create(
            "asset-1",
            new MintProductInput(
                "tenant-1",
                "SERIAL-1",
                "MODEL-1",
                "Model Name",
                Instant.parse("2026-03-01T00:00:00Z"),
                null,
                null,
                null
            ),
            now
        );

        Instant retiredAt = Instant.parse("2026-03-16T01:00:00Z");
        asset.retire(retiredAt);

        assertEquals(AssetState.RETIRED, asset.getAssetState());
        assertEquals(retiredAt, asset.getRetiredAt());
        assertEquals(RiskFlag.NONE, asset.getRiskFlag());
    }

    @Test
    void retire_failsWhenAlreadyRetired() {
        Instant now = Instant.parse("2026-03-16T00:00:00Z");
        ProductAsset asset = ProductAsset.create(
            "asset-1",
            new MintProductInput(
                "tenant-1",
                "SERIAL-1",
                "MODEL-1",
                "Model Name",
                Instant.parse("2026-03-01T00:00:00Z"),
                null,
                null,
                null
            ),
            now
        );
        asset.retire(now.plusSeconds(60));

        ProductDomainException ex = assertThrows(ProductDomainException.class, () -> asset.retire(now.plusSeconds(120)));
        assertEquals(ProductErrorCode.ASSET_ALREADY_RETIRED, ex.getErrorCode());
    }
}
