package io.attestry.product.application.service;

import io.attestry.product.application.port.DistributedPassportQueryPort;
import io.attestry.product.application.port.GroupPassportQueryPort;
import io.attestry.product.application.port.MyPassportQueryPort;
import io.attestry.product.application.port.PassportDistributionQueryPort;
import io.attestry.product.application.port.PassportOwnershipPort;
import io.attestry.product.application.port.PassportPermissionPort;
import io.attestry.product.application.port.PassportPort;
import io.attestry.product.application.port.PassportShipmentQueryPort;
import io.attestry.product.application.dto.result.AssetStateResult;
import io.attestry.product.application.dto.result.DistributedPassportDetailResult;
import io.attestry.product.application.dto.result.DistributedPassportResult;
import io.attestry.product.application.dto.result.DistributionDetailResult;
import io.attestry.product.application.dto.result.MyPassportResult;
import io.attestry.product.application.dto.result.OwnerResult;
import io.attestry.product.application.dto.result.PagedDistributedPassportResult;
import io.attestry.product.application.dto.result.PagedTenantPassportResult;
import io.attestry.product.application.dto.result.PassportDetailResult;
import io.attestry.product.application.dto.result.ShipmentDetailResult;
import io.attestry.product.application.dto.result.TenantPassportResult;
import io.attestry.product.application.port.ProductQueryPort;
import io.attestry.product.application.usecase.ProductQueryUseCase;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQueryUseCase, ProductQueryPort {

    private final PassportPort passportPort;
    private final PassportOwnershipPort ownershipPort;
    private final PassportPermissionPort permissionPort;
    private final MyPassportQueryPort myPassportQueryPort;
    private final GroupPassportQueryPort groupPassportQueryPort;
    private final DistributedPassportQueryPort distributedPassportQueryPort;
    private final PassportShipmentQueryPort shipmentQueryPort;
    private final PassportDistributionQueryPort distributionQueryPort;
    private final String publicBaseUrl;

    public ProductQueryService(
        PassportPort passportPort,
        PassportOwnershipPort ownershipPort,
        PassportPermissionPort permissionPort,
        MyPassportQueryPort myPassportQueryPort,
        GroupPassportQueryPort groupPassportQueryPort,
        DistributedPassportQueryPort distributedPassportQueryPort,
        PassportShipmentQueryPort shipmentQueryPort,
        PassportDistributionQueryPort distributionQueryPort,
        @Value("${app.product.public-base-url}") String publicBaseUrl
    ) {
        this.passportPort = passportPort;
        this.ownershipPort = ownershipPort;
        this.permissionPort = permissionPort;
        this.myPassportQueryPort = myPassportQueryPort;
        this.groupPassportQueryPort = groupPassportQueryPort;
        this.distributedPassportQueryPort = distributedPassportQueryPort;
        this.shipmentQueryPort = shipmentQueryPort;
        this.distributionQueryPort = distributionQueryPort;
        this.publicBaseUrl = publicBaseUrl;
    }

    // --- ProductQueryUseCase ---

    @Override
    public AssetStateResult getAssetState(String passportId) {
        ProductPassport passport = findPassport(passportId);
        return new AssetStateResult(
            passport.getAsset().getAssetId(),
            passportId,
            passport.getAsset().getAssetState().name(),
            passport.getAsset().getRiskFlag().name()
        );
    }

    @Override
    public OwnerResult getCurrentOwner(String passportId) {
        PassportOwnership ownership = ownershipPort.findByPassportId(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Ownership not found for passport: " + passportId));
        return new OwnerResult(passportId, ownership.getOwnerId(), ownership.getUpdatedAt());
    }

    @Override
    public boolean hasActivePermission(String passportId, String sellerTenantId) {
        return permissionPort.existsActiveByPassportAndSellerTenant(passportId, sellerTenantId);
    }

    @Override
    public PassportDetailResult getTenantPassportDetail(String tenantId, String passportId) {
        ProductPassport passport = findPassport(passportId);
        if (!passport.getTenantId().equals(tenantId)) {
            throw new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found for tenant: " + passportId);
        }
        ProductAsset asset = passport.getAsset();
        String publicUrl = publicBaseUrl + "/products/passports/" + passportId;
        ShipmentDetailResult shipment = shipmentQueryPort.findLatestShipmentByPassportId(passportId)
            .map(ShipmentDetailResult::from)
            .orElse(null);
        DistributionDetailResult distribution = distributionQueryPort.findLatestDistribution(passportId)
            .map(v -> new DistributionDetailResult(
                v.distributionId(), v.targetTenantId(), v.targetTenantName(),
                v.targetTenantType(), v.partnerLinkId(), v.status(), v.distributedAt()
            ))
            .orElse(null);

        return new PassportDetailResult(
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
            shipment,
            distribution
        );
    }

    @Override
    public DistributedPassportDetailResult getDistributedPassportDetail(String tenantId, String passportId) {
        DistributedPassportQueryPort.DistributedPassportDetailView detail = distributedPassportQueryPort.findDetailByRetailAccess(
            tenantId,
            passportId
        );
        return new DistributedPassportDetailResult(
            detail.passportId(),
            detail.qrPublicCode(),
            detail.serialNumber(),
            detail.modelId(),
            detail.modelName(),
            detail.assetState(),
            detail.riskFlag(),
            detail.manufacturedAt(),
            detail.productionBatch(),
            detail.factoryCode()
        );
    }

    @Override
    public List<MyPassportResult> listMyPassports(String ownerId) {
        return myPassportQueryPort.findByOwnerId(ownerId).stream()
            .map(v -> new MyPassportResult(
                v.passportId(), v.qrPublicCode(), v.tenantId(),
                v.assetId(), v.serialNumber(), v.modelName(),
                v.assetState(), v.riskFlag(), v.ownedSince()
            ))
            .toList();
    }

    @Override
    public PagedTenantPassportResult listTenantPassports(
        String tenantId,
        int page,
        int size,
        String assetState,
        Instant createdFrom,
        Instant createdTo,
        String keyword
    ) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new ProductDomainException(ProductErrorCode.INVALID_REQUEST, "createdFrom must be before createdTo");
        }
        String normalizedAssetState = assetState == null || assetState.isBlank() ? null : assetState.trim();
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        GroupPassportQueryPort.PagedResult paged = groupPassportQueryPort.findByTenant(
            tenantId,
            page,
            size,
            normalizedAssetState,
            createdFrom,
            createdTo,
            normalizedKeyword
        );
        List<TenantPassportResult> content = paged.content().stream()
            .map(v -> new TenantPassportResult(
                v.passportId(), v.serialNumber(), v.modelId(), v.modelName(),
                v.assetState(), v.createdAt()
            ))
            .toList();
        return new PagedTenantPassportResult(content, paged.page(), paged.size(), paged.totalElements(), paged.totalPages());
    }

    @Override
    public PagedDistributedPassportResult listDistributedPassports(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    ) {
        DistributedPassportQueryPort.PagedResult paged = distributedPassportQueryPort.findByTargetTenant(
            tenantId,
            page,
            size,
            normalize(keyword),
            normalize(sourceTenantId)
        );
        return new PagedDistributedPassportResult(
            paged.content().stream()
                .map(ProductQueryService::toDistributedPassportResult)
                .toList(),
            paged.page(),
            paged.size(),
            paged.totalElements(),
            paged.totalPages()
        );
    }

    // --- ProductQueryPort ---

    @Override
    public AssetStateView queryAssetState(String passportId) {
        ProductPassport passport = findPassport(passportId);
        return new AssetStateView(passport.getAsset().getAssetId(), passport.getAsset().getAssetState(), passport.getAsset().getRiskFlag());
    }

    @Override
    public String getCurrentOwnerId(String passportId) {
        return ownershipPort.findByPassportId(passportId)
            .map(PassportOwnership::getOwnerId)
            .orElse(null);
    }

    private ProductPassport findPassport(String passportId) {
        return passportPort.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static DistributedPassportResult toDistributedPassportResult(
        DistributedPassportQueryPort.DistributedPassportView view
    ) {
        return new DistributedPassportResult(
            view.passportId(),
            view.qrPublicCode(),
            view.assetId(),
            view.serialNumber(),
            view.modelId(),
            view.modelName(),
            view.assetState(),
            view.riskFlag(),
            view.permissionId(),
            view.expiresAt(),
            view.sourceTenantId(),
            view.targetTenantId(),
            view.permissionStatus(),
            view.distributedAt()
        );
    }

}
