package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.domain.passport.repository.PassportRepository;
import io.attestry.product.infrastructure.persistence.jpa.mapper.PassportMapper;
import io.attestry.product.infrastructure.persistence.jpa.repository.ProductAssetJpaRepository;
import io.attestry.product.infrastructure.persistence.jpa.repository.ProductPassportJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPassportRepositoryAdapter implements PassportRepository {

    private final ProductPassportJpaRepository passportJpaRepository;
    private final ProductAssetJpaRepository assetJpaRepository;
    private final PassportMapper mapper;

    public JpaPassportRepositoryAdapter(
        ProductPassportJpaRepository passportJpaRepository,
        ProductAssetJpaRepository assetJpaRepository,
        PassportMapper mapper
    ) {
        this.passportJpaRepository = passportJpaRepository;
        this.assetJpaRepository = assetJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProductPassport> findById(String passportId) {
        return passportJpaRepository.findById(passportId)
            .flatMap(passportEntity ->
                assetJpaRepository.findById(passportEntity.getAssetId())
                    .map(assetEntity -> mapper.toDomain(passportEntity, assetEntity))
            );
    }

    @Override
    public ProductPassport save(ProductPassport passport) {
        assetJpaRepository.save(mapper.toAssetEntity(passport));
        passportJpaRepository.save(mapper.toPassportEntity(passport));
        return passport;
    }

    @Override
    public boolean existsByTenantAndSerial(String tenantId, String serialNumber) {
        return assetJpaRepository.existsByTenantIdAndSerialNumber(tenantId, serialNumber);
    }
}
