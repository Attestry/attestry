package io.attestry.product.application.port;

import io.attestry.product.domain.passport.model.AssetState;
import io.attestry.product.domain.passport.model.RiskFlag;

public interface ProductQueryPort {

    AssetStateView queryAssetState(String passportId);

    boolean hasActivePermission(String passportId, String sellerTenantId);

    String getCurrentOwnerId(String passportId);

    record AssetStateView(String assetId, AssetState assetState, RiskFlag riskFlag) {
    }
}
