package io.attestry.product.infrastructure.persistence.jdbc.projection;

import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcProductRetailAccessProjectionWriter implements ProductRetailAccessProjectionWritePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcProductRetailAccessProjectionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void refreshB2cTransferAccess(RetailAccessPayload payload, String sourceEventId, Instant updatedAt) {
        Timestamp timestamp = Timestamp.from(updatedAt);

        if (payload.tenantId() == null || payload.tenantId().isBlank()
            || payload.completedAt() == null) {
            return;
        }

        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO product_retail_access_projection (
                    tenant_id,
                    passport_id,
                    access_source_type,
                    access_source_id,
                    source_tenant_id,
                    permission_id,
                    expires_at,
                    access_status,
                    granted_at,
                    updated_at
                ) VALUES (?, ?, 'B2C_TRANSFER', ?, NULL, NULL, NULL, 'COMPLETED', ?, ?)
                ON CONFLICT (tenant_id, passport_id, access_source_type, access_source_id) DO UPDATE SET
                    access_status = EXCLUDED.access_status,
                    granted_at = EXCLUDED.granted_at,
                    updated_at = EXCLUDED.updated_at
            """,
            payload.tenantId(),
            payload.passportId(),
            payload.transferId(),
            Timestamp.from(payload.completedAt()),
            timestamp
        );
    }

    @Override
    public void syncPermissionAccess(String permissionId, String sourceEventId, Instant updatedAt) {
        Timestamp timestamp = Timestamp.from(updatedAt);

        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO product_retail_access_projection (
                    tenant_id,
                    passport_id,
                    access_source_type,
                    access_source_id,
                    source_tenant_id,
                    permission_id,
                    expires_at,
                    access_status,
                    granted_at,
                    updated_at
                )
                SELECT pp.target_tenant_id,
                       pp.passport_id,
                       'PERMISSION',
                       pp.permission_id,
                       pp.source_tenant_id,
                       pp.permission_id,
                       pp.expires_at,
                       CASE
                           WHEN pp.status = 'ACTIVE'
                            AND (pp.expires_at IS NULL OR pp.expires_at > CURRENT_TIMESTAMP)
                           THEN 'ACTIVE'
                           WHEN pp.status = 'ACTIVE'
                           THEN 'EXPIRED'
                           ELSE pp.status
                       END,
                       pp.created_at,
                       ?
                FROM passport_permissions pp
                WHERE pp.permission_id = ?
                  AND pp.target_tenant_id IS NOT NULL
                ON CONFLICT (tenant_id, passport_id, access_source_type, access_source_id) DO UPDATE SET
                    source_tenant_id = EXCLUDED.source_tenant_id,
                    permission_id = EXCLUDED.permission_id,
                    expires_at = EXCLUDED.expires_at,
                    access_status = EXCLUDED.access_status,
                    granted_at = EXCLUDED.granted_at,
                    updated_at = EXCLUDED.updated_at
            """,
            timestamp,
            permissionId
        );
    }
}
