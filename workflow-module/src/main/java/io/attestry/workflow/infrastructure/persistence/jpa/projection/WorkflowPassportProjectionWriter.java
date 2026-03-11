package io.attestry.workflow.infrastructure.persistence.jpa.projection;

import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkflowPassportProjectionWriter implements WorkflowPassportProjectionWritePort {

    private final JdbcTemplate jdbcTemplate;

    public WorkflowPassportProjectionWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void refreshStateAndCatalog(String passportId, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        Timestamp timestamp = Timestamp.from(updatedAt);

        jdbcTemplate.update(
            """
                INSERT INTO workflow_passport_state_projection (
                    passport_id,
                    tenant_id,
                    asset_id,
                    asset_state,
                    risk_flag,
                    current_owner_id,
                    source_event_id,
                    source_event_version,
                    updated_at
                )
                SELECT pp.passport_id,
                       pp.tenant_id,
                       pp.asset_id,
                       pa.asset_state,
                       pa.risk_flag,
                       po.owner_id,
                       ?,
                       ?,
                       ?
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                LEFT JOIN passport_ownership po ON po.passport_id = pp.passport_id
                WHERE pp.passport_id = ?
                ON CONFLICT (passport_id) DO UPDATE SET
                    tenant_id = EXCLUDED.tenant_id,
                    asset_id = EXCLUDED.asset_id,
                    asset_state = EXCLUDED.asset_state,
                    risk_flag = EXCLUDED.risk_flag,
                    current_owner_id = EXCLUDED.current_owner_id,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            sourceEventId,
            sourceEventVersion,
            timestamp,
            passportId
        );

        jdbcTemplate.update(
            """
                INSERT INTO workflow_passport_catalog_projection (
                    passport_id,
                    asset_id,
                    serial_number,
                    model_id,
                    model_name,
                    production_batch,
                    factory_code,
                    manufactured_at,
                    source_event_id,
                    source_event_version,
                    updated_at
                )
                SELECT pp.passport_id,
                       pp.asset_id,
                       pa.serial_number,
                       pa.model_id,
                       pa.model_name,
                       pa.production_batch,
                       pa.factory_code,
                       pa.manufactured_at,
                       ?,
                       ?,
                       ?
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                WHERE pp.passport_id = ?
                ON CONFLICT (passport_id) DO UPDATE SET
                    asset_id = EXCLUDED.asset_id,
                    serial_number = EXCLUDED.serial_number,
                    model_id = EXCLUDED.model_id,
                    model_name = EXCLUDED.model_name,
                    production_batch = EXCLUDED.production_batch,
                    factory_code = EXCLUDED.factory_code,
                    manufactured_at = EXCLUDED.manufactured_at,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            sourceEventId,
            sourceEventVersion,
            timestamp,
            passportId
        );
    }

    @Override
    public void upsertOwnership(String passportId, String ownerId, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        jdbcTemplate.update(
            """
                INSERT INTO workflow_passport_ownership_projection (
                    passport_id,
                    owner_id,
                    source_event_id,
                    source_event_version,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (passport_id) DO UPDATE SET
                    owner_id = EXCLUDED.owner_id,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            passportId,
            ownerId,
            sourceEventId,
            sourceEventVersion,
            Timestamp.from(updatedAt)
        );

        jdbcTemplate.update(
            """
                UPDATE workflow_passport_state_projection
                SET current_owner_id = ?,
                    source_event_id = ?,
                    source_event_version = ?,
                    updated_at = ?
                WHERE passport_id = ?
            """,
            ownerId,
            sourceEventId,
            sourceEventVersion,
            Timestamp.from(updatedAt),
            passportId
        );
    }

    @Override
    public void syncPermissionById(String permissionId, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        jdbcTemplate.update(
            """
                INSERT INTO workflow_passport_permission_projection (
                    permission_id,
                    passport_id,
                    seller_tenant_id,
                    status,
                    expires_at,
                    source_event_id,
                    source_event_version,
                    updated_at
                )
                SELECT pp.permission_id,
                       pp.passport_id,
                       pp.seller_tenant_id,
                       pp.status,
                       pp.expires_at,
                       ?,
                       ?,
                       ?
                FROM passport_permissions pp
                WHERE pp.permission_id = ?
                  AND pp.seller_tenant_id IS NOT NULL
                ON CONFLICT (permission_id) DO UPDATE SET
                    passport_id = EXCLUDED.passport_id,
                    seller_tenant_id = EXCLUDED.seller_tenant_id,
                    status = EXCLUDED.status,
                    expires_at = EXCLUDED.expires_at,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            sourceEventId,
            sourceEventVersion,
            Timestamp.from(updatedAt),
            permissionId
        );
    }

    @Override
    public void revokeServiceRequestPermissions(String linkedServiceRequestId, String sourceEventId, Instant updatedAt) {
        jdbcTemplate.update(
            """
                UPDATE workflow_passport_permission_projection
                SET status = 'REVOKED',
                    source_event_id = ?,
                    source_event_version = NULL,
                    updated_at = ?
                WHERE permission_id IN (
                    SELECT permission_id
                    FROM passport_permissions
                    WHERE linked_service_request_id = ?
                )
            """,
            sourceEventId,
            Timestamp.from(updatedAt),
            linkedServiceRequestId
        );
    }

    @Override
    public void revokeConsentPermissions(String passportId, String providerTenantId, String sourceEventId, Instant updatedAt) {
        jdbcTemplate.update(
            """
                UPDATE workflow_passport_permission_projection
                SET status = 'REVOKED',
                    source_event_id = ?,
                    source_event_version = NULL,
                    updated_at = ?
                WHERE passport_id = ?
                  AND seller_tenant_id = ?
            """,
            sourceEventId,
            Timestamp.from(updatedAt),
            passportId,
            providerTenantId
        );
    }
}
