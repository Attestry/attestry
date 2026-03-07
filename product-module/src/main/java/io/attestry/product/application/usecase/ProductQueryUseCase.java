package io.attestry.product.application.usecase;

import io.attestry.product.application.port.PassportShipmentQueryPort;
import java.time.Instant;
import java.util.List;

public interface ProductQueryUseCase {

    AssetStateResponse getAssetState(String passportId);

    OwnerResponse getCurrentOwner(String passportId);

    boolean hasActivePermission(String passportId, String sellerTenantId);

    List<MyPassportResponse> listMyPassports(String ownerId);

    PassportDetailResponse getTenantPassportDetail(String tenantId, String passportId);

    PagedTenantPassportResponse listTenantPassports(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    );

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

    record TenantPassportResponse(
        String passportId,
        String serialNumber,
        String modelId,
        String modelName,
        String assetState,
        Instant createdAt
    ) {
    }

    record PagedTenantPassportResponse(
        List<TenantPassportResponse> content,
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
        Instant createdAt,
        String publicUrl,
        ShipmentDetailResponse shipment
    ) {
    }

    record ShipmentDetailResponse(
        String shipmentId,
        String status,
        int shipmentRound,
        Instant releasedAt,
        String releasedByUserId,
        Instant returnedAt,
        String returnedByUserId,
        List<EvidenceFileResponse> evidenceFiles
    ) {
        public static ShipmentDetailResponse from(PassportShipmentQueryPort.ShipmentView view) {
            List<EvidenceFileResponse> files = view.evidenceFiles().stream()
                .map(e -> new EvidenceFileResponse(
                    e.evidenceId(), e.originalFileName(), e.contentType(), e.sizeBytes(), e.downloadUrl()
                ))
                .toList();
            return new ShipmentDetailResponse(
                view.shipmentId(), view.status(), view.shipmentRound(),
                view.releasedAt(), view.releasedByUserId(),
                view.returnedAt(), view.returnedByUserId(),
                files
            );
        }
    }

    record EvidenceFileResponse(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }
}
