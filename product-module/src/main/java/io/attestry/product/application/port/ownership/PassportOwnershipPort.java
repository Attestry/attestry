package io.attestry.product.application.port.ownership;

import io.attestry.product.domain.ownership.model.PassportOwnership;
import java.util.Optional;

public interface PassportOwnershipPort {

    Optional<PassportOwnership> findByPassportId(String passportId);

    PassportOwnership save(PassportOwnership ownership);
}
