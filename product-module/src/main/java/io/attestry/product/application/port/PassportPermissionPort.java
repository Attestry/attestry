package io.attestry.product.application.port;

import io.attestry.product.domain.permission.model.PassportPermission;
import java.util.List;
import java.util.Optional;

public interface PassportPermissionPort {

    Optional<PassportPermission> findById(String permissionId);

    List<PassportPermission> findByPassportId(String passportId);

    PassportPermission save(PassportPermission permission);

    boolean existsActiveByPassportAndSellerTenant(String passportId, String sellerTenantId);
}
