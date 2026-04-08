package io.attestry.product.application.query;

import io.attestry.product.application.query.view.AssetStateView;
import io.attestry.product.application.query.view.DistributedPassportDetailView;
import io.attestry.product.application.query.view.MyPassportView;
import io.attestry.product.application.query.view.OwnerView;
import io.attestry.product.application.query.view.PagedDistributedPassportView;
import io.attestry.product.application.query.view.PagedTenantPassportView;
import io.attestry.product.application.query.view.PassportDetailView;
import io.attestry.product.application.port.query.DistributedPassportQueryPort;
import io.attestry.product.application.port.query.GroupPassportQueryPort;
import io.attestry.product.application.port.query.MyPassportQueryPort;
import io.attestry.product.application.port.query.PassportDistributionQueryPort;
import io.attestry.product.application.port.ownership.PassportOwnershipPort;
import io.attestry.product.application.port.permission.PassportPermissionPort;
import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import io.attestry.product.application.query.internal.ProductQueryViewAssembler;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.infrastructure.config.ProductProperties;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQueryUseCase {

    private final PassportPort passportPort;
    private final PassportOwnershipPort ownershipPort;
    private final PassportPermissionPort permissionPort;
    private final MyPassportQueryPort myPassportQueryPort;
    private final GroupPassportQueryPort groupPassportQueryPort;
    private final DistributedPassportQueryPort distributedPassportQueryPort;
    private final PassportShipmentQueryPort shipmentQueryPort;
    private final PassportDistributionQueryPort distributionQueryPort;
    private final ProductQueryViewAssembler viewAssembler;
    private final ProductProperties productProperties;

    @Override
    public AssetStateView getAssetState(String passportId) {
        ProductPassport passport = findPassport(passportId);
        return new AssetStateView(
            passport.getAsset().getAssetId(),
            passportId,
            passport.getAsset().getAssetState().name(),
            passport.getAsset().getRiskFlag().name()
        );
    }

    @Override
    public OwnerView getCurrentOwner(String passportId) {
        PassportOwnership ownership = ownershipPort.findByPassportId(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Ownership not found for passport: " + passportId));
        return new OwnerView(passportId, ownership.getOwnerId(), ownership.getUpdatedAt());
    }

    @Override
    public boolean hasActivePermission(String passportId, String sellerTenantId) {
        return permissionPort.existsActiveByPassportAndSellerTenant(passportId, sellerTenantId);
    }

    @Override
    public PassportDetailView getTenantPassportDetail(String tenantId, String passportId) {
        ProductPassport passport = findPassport(passportId);
        if (!passport.getTenantId().equals(tenantId)) {
            throw new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found for tenant: " + passportId);
        }
        String publicUrl = productProperties.getPublicBaseUrl() + "/products/passports/" + passportId;
        return viewAssembler.toPassportDetailView(
            passport,
            publicUrl,
            shipmentQueryPort.findLatestShipmentByPassportId(passportId).orElse(null),
            distributionQueryPort.findLatestDistribution(passportId).orElse(null)
        );
    }

    @Override
    public DistributedPassportDetailView getDistributedPassportDetail(String tenantId, String passportId) {
        return distributedPassportQueryPort.findDetailByRetailAccess(tenantId, passportId);
    }

    @Override
    public DistributedPassportDetailView getCompletedTransferDetail(String tenantId, String passportId) {
        return distributedPassportQueryPort.findDetailByCompletedTransfer(tenantId, passportId);
    }

    @Override
    public List<MyPassportView> listMyPassports(String ownerId) {
        return myPassportQueryPort.findByOwnerId(ownerId);
    }

    @Override
    public PagedTenantPassportView listTenantPassports(
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
        return viewAssembler.toPagedTenantPassportView(paged);
    }

    @Override
    public PagedDistributedPassportView listDistributedPassports(
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
        return viewAssembler.toPagedDistributedPassportView(paged);
    }

    private ProductPassport findPassport(String passportId) {
        return passportPort.findById(passportId)
            .orElseThrow(() -> new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Passport not found: " + passportId));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
