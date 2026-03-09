package io.attestry.product.application.service;

import io.attestry.product.application.port.DistributedPassportQueryPort;
import io.attestry.product.application.port.GroupPassportQueryPort;
import io.attestry.product.application.port.MyPassportQueryPort;
import io.attestry.product.application.port.PassportDistributionQueryPort;
import io.attestry.product.application.port.PassportShipmentQueryPort;
import io.attestry.product.application.port.ProductQueryPort;
import io.attestry.product.application.usecase.ProductQueryUseCase;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.ownership.repository.PassportOwnershipRepository;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.permission.repository.PassportPermissionRepository;
import io.attestry.product.domain.passport.repository.PassportRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQueryUseCase, ProductQueryPort {

    private final PassportRepository passportRepository;
    private final PassportOwnershipRepository ownershipRepository;
    private final PassportPermissionRepository permissionRepository;
    private final MyPassportQueryPort myPassportQueryPort;
    private final GroupPassportQueryPort groupPassportQueryPort;
    private final DistributedPassportQueryPort distributedPassportQueryPort;
    private final PassportShipmentQueryPort shipmentQueryPort;
    private final PassportDistributionQueryPort distributionQueryPort;
    private final String publicBaseUrl;

    public ProductQueryService(
        PassportRepository passportRepository,
        PassportOwnershipRepository ownershipRepository,
        PassportPermissionRepository permissionRepository,
        MyPassportQueryPort myPassportQueryPort,
        GroupPassportQueryPort groupPassportQueryPort,
        DistributedPassportQueryPort distributedPassportQueryPort,
        PassportShipmentQueryPort shipmentQueryPort,
        PassportDistributionQueryPort distributionQueryPort,
        @Value("${app.product.public-base-url}") String publicBaseUrl
    ) {
        this.passportRepository = passportRepository;
        this.ownershipRepository = ownershipRepository;
        this.permissionRepository = permissionRepository;
        this.myPassportQueryPort = myPassportQueryPort;
        this.groupPassportQueryPort = groupPassportQueryPort;
        this.distributedPassportQueryPort = distributedPassportQueryPort;
        this.shipmentQueryPort = shipmentQueryPort;
        this.distributionQueryPort = distributionQueryPort;
        this.publicBaseUrl = publicBaseUrl;
    }

    // --- ProductQueryUseCase ---

    @Override
    public AssetStateResponse getAssetState(String passportId) {
        ProductPassport passport = findPassport(passportId);
        return new AssetStateResponse(
            passport.getAsset().getAssetId(),
            passportId,
            passport.getAsset().getAssetState().name(),
            passport.getAsset().getRiskFlag().name()
        );
    }

    @Override
    public OwnerResponse getCurrentOwner(String passportId) {
        PassportOwnership ownership = ownershipRepository.findByPassportId(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Ownership not found for passport: " + passportId));
        return new OwnerResponse(passportId, ownership.getOwnerId(), ownership.getUpdatedAt());
    }

    @Override
    public boolean hasActivePermission(String passportId, String sellerTenantId) {
        return permissionRepository.existsActiveByPassportAndSellerTenant(passportId, sellerTenantId);
    }

    @Override
    public PassportDetailResponse getTenantPassportDetail(String tenantId, String passportId) {
        ProductPassport passport = findPassport(passportId);
        if (!passport.getTenantId().equals(tenantId)) {
            throw new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found for tenant: " + passportId);
        }
        ProductAsset asset = passport.getAsset();
        String publicUrl = publicBaseUrl + "/products/passports/" + passportId;
        ShipmentDetailResponse shipment = shipmentQueryPort.findLatestShipmentByPassportId(passportId)
            .map(ShipmentDetailResponse::from)
            .orElse(null);
        DistributionDetailResponse distribution = distributionQueryPort.findLatestDistribution(passportId)
            .map(v -> new DistributionDetailResponse(
                v.distributionId(), v.targetTenantId(), v.targetTenantName(),
                v.targetTenantType(), v.partnerLinkId(), v.status(), v.distributedAt()
            ))
            .orElse(null);

        return new PassportDetailResponse(
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
    public DistributedPassportDetailResponse getDistributedPassportDetail(String tenantId, String passportId) {
        DistributedPassportQueryPort.DistributedPassportDetailView detail = distributedPassportQueryPort.findDetailByRetailAccess(
            tenantId,
            passportId
        );
        return new DistributedPassportDetailResponse(
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
    public List<MyPassportResponse> listMyPassports(String ownerId) {
        return myPassportQueryPort.findByOwnerId(ownerId).stream()
            .map(v -> new MyPassportResponse(
                v.passportId(), v.qrPublicCode(), v.tenantId(),
                v.assetId(), v.serialNumber(), v.modelName(),
                v.assetState(), v.riskFlag(), v.ownedSince()
            ))
            .toList();
    }

    @Override
    public PagedTenantPassportResponse listTenantPassports(
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
        List<TenantPassportResponse> content = paged.content().stream()
            .map(v -> new TenantPassportResponse(
                v.passportId(), v.serialNumber(), v.modelId(), v.modelName(),
                v.assetState(), v.createdAt()
            ))
            .toList();
        return new PagedTenantPassportResponse(content, paged.page(), paged.size(), paged.totalElements(), paged.totalPages());
    }

    @Override
    public PagedDistributedPassportResponse listDistributedPassports(
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
        return new PagedDistributedPassportResponse(
            paged.content().stream()
                .map(ProductQueryService::toDistributedPassportResponse)
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
        return ownershipRepository.findByPassportId(passportId)
            .map(PassportOwnership::getOwnerId)
            .orElse(null);
    }

    private ProductPassport findPassport(String passportId) {
        return passportRepository.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static DistributedPassportResponse toDistributedPassportResponse(
        DistributedPassportQueryPort.DistributedPassportView view
    ) {
        return new DistributedPassportResponse(
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
