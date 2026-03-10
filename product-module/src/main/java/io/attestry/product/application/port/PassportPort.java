package io.attestry.product.application.port;

import io.attestry.product.domain.passport.model.ProductPassport;
import java.util.Optional;

public interface PassportPort {

    Optional<ProductPassport> findById(String passportId);

    ProductPassport save(ProductPassport passport);

    boolean existsByTenantAndSerial(String tenantId, String serialNumber);
}
