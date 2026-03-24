package io.attestry.workflow.infrastructure.persistence.jpa.projection;

import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class WorkflowPassportPermissionProjectionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    void syncPermissionById(
        String permissionId,
        String sourceEventId,
        Long sourceEventVersion,
        Timestamp updatedAt
    ) {
        jdbcTemplate.getJdbcOperations().update(
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
            updatedAt,
            permissionId
        );
    }

    void revokeByServiceRequest(
        String linkedServiceRequestId,
        String sourceEventId,
        Timestamp updatedAt
    ) {
        jdbcTemplate.getJdbcOperations().update(
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
            updatedAt,
            linkedServiceRequestId
        );
    }

    void revokeByConsent(
        String passportId,
        String providerTenantId,
        String sourceEventId,
        Timestamp updatedAt
    ) {
        jdbcTemplate.getJdbcOperations().update(
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
            updatedAt,
            passportId,
            providerTenantId
        );
    }
}
