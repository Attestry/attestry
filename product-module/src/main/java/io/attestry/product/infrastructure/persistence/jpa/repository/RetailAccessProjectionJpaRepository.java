package io.attestry.product.infrastructure.persistence.jpa.repository;

import io.attestry.product.infrastructure.persistence.jpa.entity.RetailAccessProjectionId;
import io.attestry.product.infrastructure.persistence.jpa.entity.RetailAccessProjectionJpaEntity;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RetailAccessProjectionJpaRepository
    extends JpaRepository<RetailAccessProjectionJpaEntity, RetailAccessProjectionId>,
            RetailAccessCustomRepository {

    @Query(value = """
        SELECT pp.passport_id AS passportId,
               pp.qr_public_code AS qrPublicCode,
               pa.serial_number AS serialNumber,
               pa.model_id AS modelId,
               pa.model_name AS modelName,
               pa.asset_state AS assetState,
               pa.risk_flag AS riskFlag,
               pa.manufactured_at AS manufacturedAt,
               pa.production_batch AS productionBatch,
               pa.factory_code AS factoryCode,
               prap.access_source_type AS accessSourceType,
               prap.access_source_id AS accessSourceId,
               prap.updated_at AS updatedAt
        FROM product_retail_access_projection prap
        JOIN product_passports pp ON pp.passport_id = prap.passport_id
        JOIN product_assets pa ON pa.asset_id = pp.asset_id
        WHERE prap.tenant_id = :tenantId
          AND prap.passport_id = :passportId
          AND prap.access_status = 'ACTIVE'
        ORDER BY prap.granted_at DESC
        LIMIT 1
        """, nativeQuery = true)
    List<RetailAccessDetailProjection> findAccessiblePassportDetailByTenantAndPassport(
        @Param("tenantId") String tenantId,
        @Param("passportId") String passportId
    );

    interface RetailAccessDetailProjection {
        String getPassportId();
        String getQrPublicCode();
        String getSerialNumber();
        String getModelId();
        String getModelName();
        String getAssetState();
        String getRiskFlag();
        Timestamp getManufacturedAt();
        String getProductionBatch();
        String getFactoryCode();
        String getAccessSourceType();
        String getAccessSourceId();
        Timestamp getUpdatedAt();
    }
}
