package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.PassportPermissionJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassportPermissionJpaRepository extends JpaRepository<PassportPermissionJpaEntity, String> {

    List<PassportPermissionJpaEntity> findByPassportId(String passportId);

    @Query("SELECT COUNT(p) > 0 FROM PassportPermissionJpaEntity p " +
           "WHERE p.passportId = :passportId AND p.sellerGroupId = :sellerGroupId AND p.status = 'ACTIVE' " +
           "AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)")
    boolean existsActiveByPassportIdAndSellerGroupId(
        @Param("passportId") String passportId,
        @Param("sellerGroupId") String sellerGroupId
    );
}
