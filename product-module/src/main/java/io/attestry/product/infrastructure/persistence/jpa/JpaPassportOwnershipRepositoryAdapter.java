package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.application.port.PassportOwnershipPort;
import io.attestry.product.domain.ownership.model.PassportOwnership;
import io.attestry.product.infrastructure.persistence.jpa.mapper.PassportOwnershipMapper;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportOwnershipJpaRepository;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class JpaPassportOwnershipRepositoryAdapter implements PassportOwnershipPort {

    private final PassportOwnershipJpaRepository jpaRepository;
    private final PassportOwnershipMapper mapper;


    @Override
    public Optional<PassportOwnership> findByPassportId(String passportId) {
        return jpaRepository.findById(passportId).map(mapper::toDomain);
    }

    @Override
    public PassportOwnership save(PassportOwnership ownership) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(ownership)));
    }
}
