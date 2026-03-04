package io.attestry.product.domain.permission.repository;

import io.attestry.product.domain.permission.model.PassportPermission;
import java.util.List;
import java.util.Optional;

public interface PassportPermissionRepository {

    Optional<PassportPermission> findById(String permissionId);

    List<PassportPermission> findByPassportId(String passportId);

    PassportPermission save(PassportPermission permission);

    boolean existsActiveByPassportAndSellerGroup(String passportId, String sellerGroupId);
}
