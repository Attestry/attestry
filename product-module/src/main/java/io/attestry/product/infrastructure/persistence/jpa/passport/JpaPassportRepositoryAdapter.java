package io.attestry.product.infrastructure.persistence.jpa.passport;

import io.attestry.product.application.port.passport.PassportPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.product.domain.passport.model.ProductPassport;
import io.attestry.product.infrastructure.persistence.jpa.mapper.PassportMapper;
import io.attestry.product.infrastructure.persistence.jpa.repository.ProductAssetJpaRepository;
import io.attestry.product.infrastructure.persistence.jpa.repository.ProductPassportJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaPassportRepositoryAdapter implements PassportPort {

    private final ProductPassportJpaRepository passportJpaRepository;
    private final ProductAssetJpaRepository assetJpaRepository;
    private final PassportMapper mapper;


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
        try {
            assetJpaRepository.saveAndFlush(mapper.toAssetEntity(passport));
            passportJpaRepository.saveAndFlush(mapper.toPassportEntity(passport));
            return passport;
        } catch (DataIntegrityViolationException ex) {
            throw new ProductDomainException(
                ProductErrorCode.DUPLICATE_SERIAL_NUMBER,
                "Duplicate serial number: " + passport.getAsset().getSerialNumber(),
                ex
            );
        }
    }

    @Override
    public boolean existsByTenantAndSerial(String tenantId, String serialNumber) {
        return assetJpaRepository.existsByTenantIdAndSerialNumber(tenantId, serialNumber);
    }
}
