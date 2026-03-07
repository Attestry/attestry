package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.domain.passport.model.AssetState;
import io.attestry.product.domain.passport.model.ProductAsset;
import io.attestry.product.domain.passport.model.RiskFlag;
import io.attestry.product.domain.passport.model.VoidReason;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.repository.PassportRepository;
import io.attestry.product.infrastructure.persistence.jpa.entity.ProductAssetJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.entity.ProductPassportJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.repository.ProductAssetJpaRepository;
import io.attestry.product.infrastructure.persistence.jpa.repository.ProductPassportJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPassportRepositoryAdapter implements PassportRepository {

    private final ProductPassportJpaRepository passportJpaRepository;
    private final ProductAssetJpaRepository assetJpaRepository;

    public JpaPassportRepositoryAdapter(
        ProductPassportJpaRepository passportJpaRepository,
        ProductAssetJpaRepository assetJpaRepository
    ) {
        this.passportJpaRepository = passportJpaRepository;
        this.assetJpaRepository = assetJpaRepository;
    }

    @Override
    public Optional<ProductPassport> findById(String passportId) {
        return passportJpaRepository.findById(passportId)
            .flatMap(passportEntity ->
                assetJpaRepository.findById(passportEntity.getAssetId())
                    .map(assetEntity -> toDomain(passportEntity, assetEntity))
            );
    }

    @Override
    public ProductPassport save(ProductPassport passport) {
        ProductAssetJpaEntity assetEntity = toAssetEntity(passport);
        ProductPassportJpaEntity passportEntity = toPassportEntity(passport);

        assetJpaRepository.save(assetEntity);
        passportJpaRepository.save(passportEntity);

        return passport;
    }

    @Override
    public boolean existsByTenantAndSerial(String tenantId, String serialNumber) {
        return assetJpaRepository.existsByTenantIdAndSerialNumber(tenantId, serialNumber);
    }

    // --- Mapping ---

    private ProductPassport toDomain(ProductPassportJpaEntity pe, ProductAssetJpaEntity ae) {
        ProductAsset asset = ProductAsset.reconstitute(
            ae.getAssetId(),
            ae.getSerialNumber(),
            ae.getModelId(),
            ae.getModelName(),
            ae.getManufacturedAt(),
            ae.getProductionBatch(),
            ae.getFactoryCode(),
            ae.getComponentRootHash(),
            AssetState.valueOf(ae.getAssetState()),
            RiskFlag.valueOf(ae.getRiskFlag()),
            ae.getCreatedAt(),
            ae.getVoidedAt(),
            ae.getVoidedReason() != null ? VoidReason.valueOf(ae.getVoidedReason()) : null,
            ae.getVoidedNote(),
            ae.getStolenAt(),
            ae.getLostAt(),
            ae.getRiskReportedBy(),
            ae.getPoliceReportNo()
        );
        return ProductPassport.reconstitute(
            pe.getPassportId(),
            pe.getTenantId(),
            pe.getQrPublicCode(),
            asset,
            pe.getCreatedAt()
        );
    }

    private ProductAssetJpaEntity toAssetEntity(ProductPassport passport) {
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

    private ProductPassportJpaEntity toPassportEntity(ProductPassport passport) {
        return new ProductPassportJpaEntity(
            passport.getPassportId(),
            passport.getAsset().getAssetId(),
            passport.getTenantId(),
            passport.getQrPublicCode(),
            passport.getCreatedAt()
        );
    }
}
