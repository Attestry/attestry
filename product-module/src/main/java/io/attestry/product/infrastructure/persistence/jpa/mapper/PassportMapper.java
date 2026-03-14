package io.attestry.product.infrastructure.persistence.jpa.mapper;

import io.attestry.product.domain.passport.model.AssetState;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.model.RiskFlag;
import io.attestry.product.domain.passport.model.VoidReason;
import io.attestry.product.infrastructure.persistence.jpa.entity.ProductAssetJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.entity.ProductPassportJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PassportMapper {

    public ProductPassport toDomain(ProductPassportJpaEntity passportEntity, ProductAssetJpaEntity assetEntity) {
        if (passportEntity == null || assetEntity == null) {
            return null;
        }
        ProductAsset asset = ProductAsset.reconstitute(
            assetEntity.getAssetId(),
            assetEntity.getSerialNumber(),
            assetEntity.getModelId(),
            assetEntity.getModelName(),
            assetEntity.getManufacturedAt(),
            assetEntity.getProductionBatch(),
            assetEntity.getFactoryCode(),
            assetEntity.getComponentRootHash(),
            AssetState.valueOf(assetEntity.getAssetState()),
            RiskFlag.valueOf(assetEntity.getRiskFlag()),
            assetEntity.getCreatedAt(),
            assetEntity.getVoidedAt(),
            assetEntity.getVoidedReason() != null ? VoidReason.valueOf(assetEntity.getVoidedReason()) : null,
            assetEntity.getVoidedNote(),
            assetEntity.getStolenAt(),
            assetEntity.getLostAt(),
            assetEntity.getRiskReportedBy(),
            assetEntity.getPoliceReportNo()
        );
        return ProductPassport.reconstitute(
            passportEntity.getPassportId(),
            passportEntity.getTenantId(),
            passportEntity.getQrPublicCode(),
            asset,
            passportEntity.getCreatedAt()
        );
    }

    public ProductAssetJpaEntity toAssetEntity(ProductPassport passport) {
        ProductAsset asset = passport.getAsset();
        return new ProductAssetJpaEntity(
            asset.getAssetId(),
            passport.getTenantId(),
            asset.getSerialNumber(),
            asset.getModelId(),
            asset.getModelName(),
            asset.getManufacturedAt(),
            asset.getProductionBatch(),
            asset.getFactoryCode(),
            asset.getComponentRootHash(),
            asset.getAssetState().name(),
            asset.getRiskFlag().name(),
            asset.getCreatedAt(),
            asset.getVoidedAt(),
            asset.getVoidedReason() != null ? asset.getVoidedReason().name() : null,
            asset.getVoidedNote(),
            asset.getStolenAt(),
            asset.getLostAt(),
            asset.getRiskReportedBy(),
            asset.getPoliceReportNo()
        );
    }

    public ProductPassportJpaEntity toPassportEntity(ProductPassport passport) {
        return new ProductPassportJpaEntity(
            passport.getPassportId(),
            passport.getAsset().getAssetId(),
            passport.getTenantId(),
            passport.getQrPublicCode(),
            passport.getCreatedAt()
        );
    }
}
