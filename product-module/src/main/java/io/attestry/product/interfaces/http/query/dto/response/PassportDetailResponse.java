package io.attestry.product.interfaces.http.query.dto.response;

import io.attestry.product.application.dto.view.DistributionDetailView;
import io.attestry.product.application.dto.view.PassportDetailView;
import io.attestry.product.application.dto.view.ShipmentDetailView;
import java.time.Instant;
import java.util.List;

public record PassportDetailResponse(
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
    ShipmentResponse shipment,
    DistributionResponse distribution
) {
    public static PassportDetailResponse from(PassportDetailView result) {
        ShipmentResponse shipmentResponse = result.shipment() != null
            ? ShipmentResponse.from(result.shipment())
            : null;
        DistributionResponse distributionResponse = result.distribution() != null
            ? DistributionResponse.from(result.distribution())
            : null;
        return new PassportDetailResponse(
            result.passportId(), result.qrPublicCode(), result.tenantId(),
            result.assetId(), result.serialNumber(), result.modelId(), result.modelName(),
            result.manufacturedAt(), result.productionBatch(), result.factoryCode(),
            result.assetState(), result.riskFlag(), result.createdAt(), result.publicUrl(),
            shipmentResponse, distributionResponse
        );
    }

    public record ShipmentResponse(
        String shipmentId,
        String status,
        int shipmentRound,
        Instant releasedAt,
        String releasedByUserEmail,
        Instant returnedAt,
        String returnedByUserEmail,
        List<EvidenceFileResponse> evidenceFiles
    ) {
        public static ShipmentResponse from(ShipmentDetailView r) {
            return new ShipmentResponse(
                r.shipmentId(), r.status(), r.shipmentRound(),
                r.releasedAt(), r.releasedByUserEmail(),
                r.returnedAt(), r.returnedByUserEmail(),
                r.evidenceFiles().stream()
                    .map(e -> new EvidenceFileResponse(
                        e.evidenceId(), e.originalFileName(), e.contentType(), e.sizeBytes(), e.downloadUrl()
                    ))
                    .toList()
            );
        }
    }

    public record EvidenceFileResponse(
        String evidenceId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        String downloadUrl
    ) {
    }

    public record DistributionResponse(
        String distributionId,
        String targetTenantId,
        String targetTenantName,
        String targetTenantType,
        String partnerLinkId,
        String status,
        Instant distributedAt
    ) {
        public static DistributionResponse from(DistributionDetailView r) {
            return new DistributionResponse(
                r.distributionId(), r.targetTenantId(), r.targetTenantName(),
                r.targetTenantType(), r.partnerLinkId(), r.status(), r.distributedAt()
            );
        }
    }
}
