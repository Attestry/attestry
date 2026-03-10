package io.attestry.product.interfaces.http.dto.response;

import io.attestry.product.application.dto.result.AssetStateResult;

public record AssetStateResponse(
    String assetId,
    String passportId,
    String assetState,
    String riskFlag
) {
    public static AssetStateResponse from(AssetStateResult result) {
        return new AssetStateResponse(result.assetId(), result.passportId(), result.assetState(), result.riskFlag());
    }
}
