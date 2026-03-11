package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.infrastructure.persistence.jpa.entity.WorkflowPassportCatalogProjectionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowPassportCatalogProjectionJpaRepository
    extends JpaRepository<WorkflowPassportCatalogProjectionEntity, String> {

    @Query(value = """
        SELECT passport_id AS passportId,
               asset_id AS assetId,
               serial_number AS serialNumber,
               model_id AS modelId,
               model_name AS modelName,
               production_batch AS productionBatch,
               factory_code AS factoryCode
        FROM workflow_passport_catalog_projection
        WHERE passport_id IN (:passportIds)
        """, nativeQuery = true)
    List<PassportAssetInfoProjection> findAssetInfoByPassportIds(@Param("passportIds") List<String> passportIds);

    interface PassportAssetInfoProjection {
        String getPassportId();
        String getAssetId();
        String getSerialNumber();
        String getModelId();
        String getModelName();
        String getProductionBatch();
        String getFactoryCode();
    }
}
