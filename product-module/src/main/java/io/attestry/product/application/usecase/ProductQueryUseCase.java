package io.attestry.product.application.usecase;

import java.time.Instant;
import java.util.List;

public interface ProductQueryUseCase {

    AssetStateResponse getAssetState(String passportId);

    OwnerResponse getCurrentOwner(String passportId);

    boolean hasActivePermission(String passportId, String sellerTenantId);

    List<MyPassportResponse> listMyPassports(String ownerId);

    PassportDetailResponse getPassportDetail(String passportId);

    PagedMintedPassportResponse listMintedPassports(String tenantId, int page, int size);

    record AssetStateResponse(String assetId, String passportId, String assetState, String riskFlag) {
    }

    record OwnerResponse(String passportId, String ownerId, Instant updatedAt) {
    }

    record MyPassportResponse(
        String passportId,
        String qrPublicCode,
        String tenantId,
        String assetId,
        String serialNumber,
        String modelName,
        String assetState,
        String riskFlag,
        Instant ownedSince
    ) {
    }

    record MintedPassportResponse(
        String passportId,
        String qrPublicCode,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String assetState,
        String riskFlag,
        String ownerId,
        Instant createdAt
    ) {
    }

    record PagedMintedPassportResponse(
        List<MintedPassportResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    record PassportDetailResponse(
        String passportId,
        String qrPublicCode,
        String tenantId,
        String assetId,
        String serialNumber,
        String modelId,
        String modelName,
        Instant manufacturedAt,
        String productionBatch,
        String factoryCode,
        String assetState,
        String riskFlag,
        String ownerId,
        Instant ownerUpdatedAt,
        Instant createdAt
    ) {
    }
}
