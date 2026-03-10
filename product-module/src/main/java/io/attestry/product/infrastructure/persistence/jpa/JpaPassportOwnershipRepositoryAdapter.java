package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.domain.ownership.repository.PassportOwnershipRepository;
import io.attestry.product.infrastructure.persistence.jpa.mapper.PassportOwnershipMapper;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportOwnershipJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPassportOwnershipRepositoryAdapter implements PassportOwnershipRepository {

    private final PassportOwnershipJpaRepository jpaRepository;
    private final PassportOwnershipMapper mapper;

    public JpaPassportOwnershipRepositoryAdapter(
        PassportOwnershipJpaRepository jpaRepository,
        PassportOwnershipMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<PassportOwnership> findByPassportId(String passportId) {
        return jpaRepository.findById(passportId).map(mapper::toDomain);
    }

    @Override
    public PassportOwnership save(PassportOwnership ownership) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(ownership)));
    }
}
