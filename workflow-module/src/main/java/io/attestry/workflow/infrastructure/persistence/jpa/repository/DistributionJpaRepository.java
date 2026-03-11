package io.attestry.workflow.infrastructure.persistence.jpa.repository;

import io.attestry.workflow.infrastructure.persistence.jpa.entity.DistributionJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DistributionJpaRepository extends JpaRepository<DistributionJpaEntity, String> {

    List<DistributionJpaEntity> findBySourceTenantId(String sourceTenantId);

    @Query(
        value = """
            SELECT d.distribution_id AS distributionId,
                   d.passport_id AS passportId,
                   d.source_tenant_id AS sourceTenantId,
                   d.target_tenant_id AS targetTenantId,
                   d.partner_link_id AS partnerLinkId,
                   d.delegation_id AS delegationId,
                   d.status AS status,
                   wpcp.serial_number AS serialNumber,
                   wpcp.model_name AS modelName,
                   d.distributed_by_user_id AS distributedByUserId,
                   d.distributed_at AS distributedAt,
                   d.recalled_by_user_id AS recalledByUserId,
                   d.recalled_at AS recalledAt,
                   d.recall_reason AS recallReason
            FROM distributions d
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = d.passport_id
            WHERE d.source_tenant_id = :sourceTenantId
              AND (
                    :keyword IS NULL
                    OR LOWER(wpcp.serial_number) LIKE CONCAT('%', :keyword, '%')
                    OR LOWER(wpcp.model_name) LIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY d.distributed_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM distributions d
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = d.passport_id
            WHERE d.source_tenant_id = :sourceTenantId
              AND (
                    :keyword IS NULL
                    OR LOWER(wpcp.serial_number) LIKE CONCAT('%', :keyword, '%')
                    OR LOWER(wpcp.model_name) LIKE CONCAT('%', :keyword, '%')
              )
        """,
        nativeQuery = true
    )
    Page<DistributionRowProjection> findDistributionRowsBySourceTenantId(
        @Param("sourceTenantId") String sourceTenantId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT d.distribution_id AS distributionId,
                   d.passport_id AS passportId,
                   d.source_tenant_id AS sourceTenantId,
                   d.target_tenant_id AS targetTenantId,
                   d.partner_link_id AS partnerLinkId,
                   d.delegation_id AS delegationId,
                   d.status AS status,
                   wpcp.serial_number AS serialNumber,
                   wpcp.model_name AS modelName,
                   d.distributed_by_user_id AS distributedByUserId,
                   d.distributed_at AS distributedAt,
                   d.recalled_by_user_id AS recalledByUserId,
                   d.recalled_at AS recalledAt,
                   d.recall_reason AS recallReason
            FROM distributions d
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = d.passport_id
            WHERE d.distribution_id = :distributionId
        """,
        nativeQuery = true
    )
    Optional<DistributionRowProjection> findDistributionRowById(@Param("distributionId") String distributionId);

    @Query(
        value = """
            SELECT d.distribution_id AS distributionId,
                   d.passport_id AS passportId,
                   d.source_tenant_id AS sourceTenantId,
                   d.target_tenant_id AS targetTenantId,
                   d.partner_link_id AS partnerLinkId,
                   d.delegation_id AS delegationId,
                   d.status AS status,
                   wpcp.serial_number AS serialNumber,
                   wpcp.model_name AS modelName,
                   d.distributed_by_user_id AS distributedByUserId,
                   d.distributed_at AS distributedAt,
                   d.recalled_by_user_id AS recalledByUserId,
                   d.recalled_at AS recalledAt,
                   d.recall_reason AS recallReason
            FROM distributions d
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = d.passport_id
            WHERE d.passport_id = :passportId
            ORDER BY d.distributed_at DESC
            LIMIT 1
        """,
        nativeQuery = true
    )
    Optional<DistributionRowProjection> findLatestDistributionRowByPassportId(@Param("passportId") String passportId);

    @Query(
        value = """
            SELECT wpcp.passport_id AS passportId,
                   wpcp.asset_id AS assetId,
                   wpcp.serial_number AS serialNumber,
                   wpcp.model_id AS modelId,
                   wpcp.model_name AS modelName,
                   wpcp.production_batch AS productionBatch,
                   wpcp.factory_code AS factoryCode
            FROM workflow_shipments ws
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = ws.passport_id
            WHERE ws.tenant_id = :tenantId
              AND ws.status = 'RELEASED'
              AND NOT EXISTS (
                    SELECT 1
                    FROM distributions d
                    WHERE d.passport_id = ws.passport_id
                      AND d.status = 'DISTRIBUTED'
              )
              AND (
                    :keyword IS NULL
                    OR LOWER(wpcp.serial_number) LIKE CONCAT('%', :keyword, '%')
                    OR LOWER(wpcp.model_name) LIKE CONCAT('%', :keyword, '%')
              )
            ORDER BY ws.released_at DESC
        """,
        countQuery = """
            SELECT COUNT(*)
            FROM workflow_shipments ws
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = ws.passport_id
            WHERE ws.tenant_id = :tenantId
              AND ws.status = 'RELEASED'
              AND NOT EXISTS (
                    SELECT 1
                    FROM distributions d
                    WHERE d.passport_id = ws.passport_id
                      AND d.status = 'DISTRIBUTED'
              )
              AND (
                    :keyword IS NULL
                    OR LOWER(wpcp.serial_number) LIKE CONCAT('%', :keyword, '%')
                    OR LOWER(wpcp.model_name) LIKE CONCAT('%', :keyword, '%')
              )
        """,
        nativeQuery = true
    )
    Page<DistributionCandidateProjection> findDistributionCandidatesByTenantId(
        @Param("tenantId") String tenantId,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    interface DistributionRowProjection {
        String getDistributionId();
        String getPassportId();
        String getSourceTenantId();
        String getTargetTenantId();
        String getPartnerLinkId();
        String getDelegationId();
        String getStatus();
        String getSerialNumber();
        String getModelName();
        String getDistributedByUserId();
        Instant getDistributedAt();
        String getRecalledByUserId();
        Instant getRecalledAt();
        String getRecallReason();
    }

    interface DistributionCandidateProjection {
        String getPassportId();
        String getAssetId();
        String getSerialNumber();
        String getModelId();
        String getModelName();
        String getProductionBatch();
        String getFactoryCode();
    }
}
