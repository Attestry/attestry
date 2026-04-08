package io.attestry.product.application.query.internal;

import io.attestry.product.application.query.view.DistributionDetailView;
import io.attestry.product.application.query.view.PagedDistributedPassportView;
import io.attestry.product.application.query.view.PagedTenantPassportView;
import io.attestry.product.application.query.view.PassportDetailView;
import io.attestry.product.application.query.view.ShipmentDetailView;
import io.attestry.product.application.query.view.TenantPassportView;
import io.attestry.product.application.port.query.DistributedPassportQueryPort;
import io.attestry.product.application.port.query.GroupPassportQueryPort;
import io.attestry.product.application.port.query.PassportDistributionQueryPort;
import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductQueryViewAssembler {

    public PassportDetailView toPassportDetailView(
        ProductPassport passport,
        String publicUrl,
        PassportShipmentQueryPort.ShipmentRecord shipment,
        PassportDistributionQueryPort.DistributionRecord distribution
    ) {
        ProductAsset asset = passport.getAsset();
        return new PassportDetailView(
            passport.getPassportId(),
            passport.getQrPublicCode(),
            passport.getTenantId(),
            asset.getAssetId(),
            asset.getSerialNumber(),
            asset.getModelId(),
            asset.getModelName(),
            asset.getManufacturedAt(),
            asset.getProductionBatch(),
            asset.getFactoryCode(),
            asset.getAssetState().name(),
            asset.getRiskFlag().name(),
            passport.getCreatedAt(),
            publicUrl,
            shipment == null ? null : ShipmentDetailView.from(shipment),
            distribution == null ? null : new DistributionDetailView(
                distribution.distributionId(),
                distribution.targetTenantId(),
                distribution.targetTenantName(),
                distribution.targetTenantType(),
                distribution.partnerLinkId(),
                distribution.status(),
                distribution.distributedAt()
            )
        );
    }

    public PagedTenantPassportView toPagedTenantPassportView(GroupPassportQueryPort.PagedResult paged) {
        List<TenantPassportView> content = paged.content().stream()
            .map(this::toTenantPassportView)
            .toList();
        return new PagedTenantPassportView(content, paged.page(), paged.size(), paged.totalElements(), paged.totalPages());
    }

    public PagedDistributedPassportView toPagedDistributedPassportView(DistributedPassportQueryPort.PagedResult paged) {
        return new PagedDistributedPassportView(
            paged.content(),
            paged.page(),
            paged.size(),
            paged.totalElements(),
            paged.totalPages()
        );
    }

    public TenantPassportView toTenantPassportView(GroupPassportQueryPort.GroupPassportRow row) {
        return new TenantPassportView(
            row.passportId(),
            row.serialNumber(),
            row.modelId(),
            row.modelName(),
            row.assetState(),
            row.createdAt()
        );
    }
}
