package io.attestry.product.domain.passport.repository;

import io.attestry.product.domain.passport.model.ProductPassport;
import java.util.Optional;

public interface PassportRepository {

    Optional<ProductPassport> findById(String passportId);

    ProductPassport save(ProductPassport passport);

    boolean existsByGroupAndSerial(String groupId, String serialNumber);
}
