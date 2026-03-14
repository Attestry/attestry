package io.attestry.product.interfaces.http.query.dto.response;

import io.attestry.product.application.dto.view.AssetStateView;

public record AssetStateResponse(
    String assetId,
    String passportId,
    String assetState,
    String riskFlag
) {
    public static AssetStateResponse from(AssetStateView view) {
        return new AssetStateResponse(view.assetId(), view.passportId(), view.assetState(), view.riskFlag());
    }
}
