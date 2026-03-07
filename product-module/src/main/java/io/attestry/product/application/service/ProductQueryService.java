package io.attestry.product.application.service;

import io.attestry.product.application.port.GroupPassportQueryPort;
import io.attestry.product.application.port.MyPassportQueryPort;
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
import java.util.List;
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

    public ProductQueryService(
        PassportRepository passportRepository,
        PassportOwnershipRepository ownershipRepository,
        PassportPermissionRepository permissionRepository,
        MyPassportQueryPort myPassportQueryPort,
        GroupPassportQueryPort groupPassportQueryPort
    ) {
        this.passportRepository = passportRepository;
        this.ownershipRepository = ownershipRepository;
        this.permissionRepository = permissionRepository;
        this.myPassportQueryPort = myPassportQueryPort;
        this.groupPassportQueryPort = groupPassportQueryPort;
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
        PassportOwnership ownership = ownershipRepository.findByPassportId(passportId).orElse(null);

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
            ownership == null ? null : ownership.getOwnerId(),
            ownership == null ? null : ownership.getUpdatedAt(),
            passport.getCreatedAt()
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
    public PagedTenantPassportResponse listTenantPassports(String tenantId, int page, int size) {
        GroupPassportQueryPort.PagedResult paged = groupPassportQueryPort.findByTenant(tenantId, page, size);
        List<TenantPassportResponse> content = paged.content().stream()
            .map(v -> new TenantPassportResponse(
                v.passportId(), v.serialNumber(), v.modelId(), v.modelName(),
                v.assetState(), v.createdAt()
            ))
            .toList();
        return new PagedTenantPassportResponse(content, paged.page(), paged.size(), paged.totalElements(), paged.totalPages());
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
}
