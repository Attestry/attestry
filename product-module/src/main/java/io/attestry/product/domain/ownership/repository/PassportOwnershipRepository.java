package io.attestry.product.domain.ownership.repository;

import io.attestry.product.domain.ownership.model.PassportOwnership;
import java.util.Optional;

public interface PassportOwnershipRepository {

    Optional<PassportOwnership> findByPassportId(String passportId);

    PassportOwnership save(PassportOwnership ownership);
}
