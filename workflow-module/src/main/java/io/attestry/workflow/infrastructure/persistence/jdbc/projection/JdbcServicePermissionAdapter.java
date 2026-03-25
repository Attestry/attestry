package io.attestry.workflow.infrastructure.persistence.jdbc.projection;

import io.attestry.workflow.application.port.servicerequest.ServicePermissionPort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcServicePermissionAdapter implements ServicePermissionPort {

    private static final String SCOPE_SERVICE_REPAIR = "SERVICE_REPAIR";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String ACTIVE_SERVICE_REPAIR_CONFLICT_TARGET =
        "scope = '" + SCOPE_SERVICE_REPAIR + "' AND status = '" + STATUS_ACTIVE + "'";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final WorkflowPassportProjectionWritePort projectionWriter;

    public JdbcServicePermissionAdapter(
        NamedParameterJdbcTemplate jdbcTemplate,
        WorkflowPassportProjectionWritePort projectionWriter
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectionWriter = projectionWriter;
    }

    @Override
    public String grantServiceRepairPermission(
        String passportId,
        String providerTenantId,
        String linkedServiceRequestId,
        String grantedByUserId,
        Instant now
    ) {
        String permissionId = UUID.randomUUID().toString();
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO passport_permissions (
                    permission_id, passport_id, seller_tenant_id,
                    scope, status,
                    linked_service_request_id, granted_by_user_id,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            permissionId,
            passportId,
            providerTenantId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE,
            linkedServiceRequestId,
            grantedByUserId,
            Timestamp.from(now)
        );
        projectionWriter.syncPermissionById(
            permissionId,
            "service-permission:grant:" + permissionId,
            null,
            now
        );
        return permissionId;
    }

    @Override
    public void revokeByServiceRequestId(String linkedServiceRequestId) {
        jdbcTemplate.getJdbcOperations().update(
            """
                UPDATE passport_permissions
                SET status = ?
                WHERE linked_service_request_id = ? AND status = ?
            """,
            STATUS_REVOKED,
            linkedServiceRequestId,
            STATUS_ACTIVE
        );
        projectionWriter.revokeServiceRequestPermissions(
            linkedServiceRequestId,
            "service-permission:revoke-by-request:" + linkedServiceRequestId,
            Instant.now()
        );
    }

    @Override
    public boolean hasActiveServiceRepairPermission(String passportId, String providerTenantId) {
        Integer count = jdbcTemplate.getJdbcOperations().queryForObject(
            """
                SELECT COUNT(1) FROM passport_permissions
                WHERE passport_id = ?
                  AND seller_tenant_id = ?
                  AND scope = ?
                  AND status = ?
            """,
            Integer.class,
            passportId,
            providerTenantId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE
        );
        return count != null && count > 0;
    }

    @Override
    public String grantServiceRepairConsent(
        String passportId,
        String providerTenantId,
        String grantedByUserId,
        Instant now
    ) {
        String permissionId = UUID.randomUUID().toString();
        jdbcTemplate.getJdbcOperations().update(
            ("""
                INSERT INTO passport_permissions (
                    permission_id, passport_id, seller_tenant_id,
                    scope, status,
                    granted_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (passport_id, seller_tenant_id)
                    WHERE %s
                DO UPDATE SET granted_by_user_id = EXCLUDED.granted_by_user_id,
                              created_at = EXCLUDED.created_at
            """).formatted(ACTIVE_SERVICE_REPAIR_CONFLICT_TARGET),
            permissionId,
            passportId,
            providerTenantId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE,
            grantedByUserId,
            Timestamp.from(now)
        );
        String actualPermissionId = findActivePermissionId(passportId, providerTenantId).orElse(permissionId);
        projectionWriter.syncPermissionById(
            actualPermissionId,
            "service-permission:grant-consent:" + actualPermissionId,
            null,
            now
        );
        return actualPermissionId;
    }

    @Override
    public void revokeConsentByPassportAndTenant(String passportId, String providerTenantId) {
        jdbcTemplate.getJdbcOperations().update(
            """
                UPDATE passport_permissions
                SET status = ?
                WHERE passport_id = ?
                  AND seller_tenant_id = ?
                  AND scope = ?
                  AND status = ?
                  AND linked_service_request_id IS NULL
            """,
            STATUS_REVOKED,
            passportId,
            providerTenantId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE
        );
        projectionWriter.revokeConsentPermissions(
            passportId,
            providerTenantId,
            "service-permission:revoke-consent:" + passportId + ":" + providerTenantId,
            Instant.now()
        );
    }

    @Override
    public void linkServiceRequest(String permissionId, String serviceRequestId) {
        jdbcTemplate.getJdbcOperations().update(
            """
                UPDATE passport_permissions
                SET linked_service_request_id = ?
                WHERE permission_id = ? AND status = ?
            """,
            serviceRequestId,
            permissionId,
            STATUS_ACTIVE
        );
    }

    @Override
    public Optional<String> findActivePermissionId(String passportId, String providerTenantId) {
        List<String> ids = jdbcTemplate.getJdbcOperations().queryForList(
            """
                SELECT permission_id FROM passport_permissions
                WHERE passport_id = ?
                  AND seller_tenant_id = ?
                  AND scope = ?
                  AND status = ?
            """,
            String.class,
            passportId,
            providerTenantId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE
        );
        return ids.stream().findFirst();
    }
}
