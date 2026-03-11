package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.PassportOwnershipJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassportOwnershipJpaRepository extends JpaRepository<PassportOwnershipJpaEntity, String> {

    @Query(value = """
        SELECT pp.passport_id AS passportId, pp.qr_public_code AS qrPublicCode, pp.tenant_id AS tenantId,
               pa.asset_id AS assetId, pa.serial_number AS serialNumber, pa.model_name AS modelName,
               pa.asset_state AS assetState, pa.risk_flag AS riskFlag,
               po.updated_at AS ownedSince
        FROM passport_ownership po
        JOIN product_passports pp ON pp.passport_id = po.passport_id
        JOIN product_assets pa ON pa.asset_id = pp.asset_id
        WHERE po.owner_id = :ownerId
        ORDER BY po.updated_at DESC
        """, nativeQuery = true)
    List<MyPassportProjection> findMyPassportsByOwnerId(@Param("ownerId") String ownerId);

    interface MyPassportProjection {
        String getPassportId();
        String getQrPublicCode();
        String getTenantId();
        String getAssetId();
        String getSerialNumber();
        String getModelName();
        String getAssetState();
        String getRiskFlag();
        java.sql.Timestamp getOwnedSince();
    }
}
