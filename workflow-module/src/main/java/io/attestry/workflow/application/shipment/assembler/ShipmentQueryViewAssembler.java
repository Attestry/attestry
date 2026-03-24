package io.attestry.workflow.application.shipment.assembler;

import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort.PassportAssetInfo;
import io.attestry.workflow.application.shipment.view.PagedReleaseCandidateView;
import io.attestry.workflow.application.shipment.view.PagedShipmentView;
import io.attestry.workflow.application.shipment.view.ShipmentDetailView;
import io.attestry.workflow.application.shipment.view.ShipmentReleaseCandidateView;
import io.attestry.workflow.application.shipment.view.ShipmentView;
import io.attestry.workflow.domain.shipment.model.Shipment;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ShipmentQueryViewAssembler {

    public PagedShipmentView toPagedShipmentView(ShipmentProductReadPort.PagedShipmentReadResult result) {
        List<ShipmentView> content = result.content().stream()
            .map(v -> new ShipmentView(
                v.shipmentId(),
                v.tenantId(),
                v.passportId(),
                v.assetId(),
                v.serialNumber(),
                v.modelId(),
                v.modelName(),
                v.productionBatch(),
                v.factoryCode(),
                v.shipmentRound(),
                v.status(),
                v.releasedAt(),
                v.releasedByUserId(),
                v.releasedByTenantId(),
                v.evidenceGroupId(),
                v.returnedAt(),
                v.returnedByUserId(),
                v.returnEvidenceGroupId(),
                v.createdAt()
            ))
            .toList();
        return new PagedShipmentView(
            content,
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }

    public PagedReleaseCandidateView toPagedReleaseCandidateView(
        ShipmentProductReadPort.PagedReleaseCandidateResult result
    ) {
        List<ShipmentReleaseCandidateView> content = result.content().stream()
            .map(candidate -> new ShipmentReleaseCandidateView(
                candidate.passportId(),
                candidate.assetId(),
                candidate.serialNumber(),
                candidate.modelId(),
                candidate.modelName(),
                candidate.productionBatch(),
                candidate.factoryCode()
            ))
            .toList();
        return new PagedReleaseCandidateView(
            content,
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        );
    }

    public ShipmentDetailView toShipmentDetailView(
        Shipment shipment,
        PassportAssetInfo assetInfo,
        Map<String, String> emailMap,
        List<ShipmentDetailView.EvidenceFileView> releaseFiles,
        List<ShipmentDetailView.EvidenceFileView> returnFiles
    ) {
        return new ShipmentDetailView(
            shipment.shipmentId(),
            shipment.tenantId(),
            shipment.passportId(),
            assetInfo != null ? assetInfo.modelName() : "",
            assetInfo != null ? assetInfo.serialNumber() : "",
            shipment.shipmentRound(),
            shipment.status().name(),
            shipment.releasedAt(),
            emailMap.getOrDefault(shipment.releasedByUserId(), null),
            shipment.returnedAt(),
            emailMap.getOrDefault(shipment.returnedByUserId(), null),
            releaseFiles,
            returnFiles,
            shipment.createdAt()
        );
    }

    public List<ShipmentView> toShipmentViews(
        List<Shipment> shipments,
        Map<String, PassportAssetInfo> assetMap
    ) {
        return shipments.stream()
            .map(shipment -> {
                PassportAssetInfo asset = assetMap.get(shipment.passportId());
                return new ShipmentView(
                    shipment.shipmentId(),
                    shipment.tenantId(),
                    shipment.passportId(),
                    asset != null ? asset.assetId() : null,
                    asset != null ? asset.serialNumber() : null,
                    asset != null ? asset.modelId() : null,
                    asset != null ? asset.modelName() : null,
                    asset != null ? asset.productionBatch() : null,
                    asset != null ? asset.factoryCode() : null,
                    shipment.shipmentRound(),
                    shipment.status().name(),
                    shipment.releasedAt(),
                    shipment.releasedByUserId(),
                    shipment.releasedByTenantId(),
                    shipment.evidenceGroupId(),
                    shipment.returnedAt(),
                    shipment.returnedByUserId(),
                    shipment.returnEvidenceGroupId(),
                    shipment.createdAt()
                );
            })
            .toList();
    }
}
