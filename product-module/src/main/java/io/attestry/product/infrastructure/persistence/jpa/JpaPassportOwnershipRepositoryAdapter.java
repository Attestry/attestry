package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.ownership.repository.PassportOwnershipRepository;
import io.attestry.product.infrastructure.persistence.jpa.entity.PassportOwnershipJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportOwnershipJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPassportOwnershipRepositoryAdapter implements PassportOwnershipRepository {

    private final PassportOwnershipJpaRepository jpaRepository;

    public JpaPassportOwnershipRepositoryAdapter(PassportOwnershipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PassportOwnership> findByPassportId(String passportId) {
        return jpaRepository.findById(passportId).map(this::toDomain);
    }

    @Override
    public PassportOwnership save(PassportOwnership ownership) {
        PassportOwnershipJpaEntity entity = toEntity(ownership);
        PassportOwnershipJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private PassportOwnershipJpaEntity toEntity(PassportOwnership ownership) {
        return new PassportOwnershipJpaEntity(
            ownership.getPassportId(),
            ownership.getOwnerId(),
            ownership.getUpdatedAt(),
            ownership.getLastLedgerSeq()
        );
    }

    private PassportOwnership toDomain(PassportOwnershipJpaEntity entity) {
        return PassportOwnership.reconstitute(
            entity.getPassportId(),
            entity.getOwnerId(),
            entity.getUpdatedAt(),
            entity.getLastLedgerSeq()
        );
    }
}
