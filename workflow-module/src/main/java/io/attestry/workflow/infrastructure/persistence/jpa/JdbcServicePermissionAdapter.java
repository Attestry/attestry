package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.ServicePermissionPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcServicePermissionAdapter implements ServicePermissionPort {

    private static final String SCOPE_SERVICE_REPAIR = "SERVICE_REPAIR";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";

    private final JdbcTemplate jdbcTemplate;

    public JdbcServicePermissionAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String grantServiceRepairPermission(
        String passportId,
        String providerGroupId,
        String linkedServiceRequestId,
        String grantedByUserId,
        Instant now
    ) {
        String permissionId = UUID.randomUUID().toString();
        jdbcTemplate.update(
            """
                INSERT INTO passport_permissions (
                    permission_id, passport_id, seller_group_id,
                    scope, status,
                    linked_service_request_id, granted_by_user_id,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            permissionId,
            passportId,
            providerGroupId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE,
            linkedServiceRequestId,
            grantedByUserId,
            Timestamp.from(now)
        );
        return permissionId;
    }

    @Override
    public void revokeByServiceRequestId(String linkedServiceRequestId) {
        jdbcTemplate.update(
            """
                UPDATE passport_permissions
                SET status = ?
                WHERE linked_service_request_id = ? AND status = ?
            """,
            STATUS_REVOKED,
            linkedServiceRequestId,
            STATUS_ACTIVE
        );
    }

    @Override
    public boolean hasActiveServiceRepairPermission(String passportId, String providerGroupId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(1) FROM passport_permissions
                WHERE passport_id = ?
                  AND seller_group_id = ?
                  AND scope = ?
                  AND status = ?
            """,
            Integer.class,
            passportId,
            providerGroupId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE
        );
        return count != null && count > 0;
    }

    @Override
    public String grantServiceRepairConsent(
        String passportId,
        String providerGroupId,
        String grantedByUserId,
        Instant now
    ) {
        String permissionId = UUID.randomUUID().toString();
        jdbcTemplate.update(
            """
                INSERT INTO passport_permissions (
                    permission_id, passport_id, seller_group_id,
                    scope, status,
                    granted_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (passport_id, seller_group_id)
                    WHERE scope = 'SERVICE_REPAIR' AND status = 'ACTIVE'
                DO UPDATE SET granted_by_user_id = EXCLUDED.granted_by_user_id,
                              created_at = EXCLUDED.created_at
            """,
            permissionId,
            passportId,
            providerGroupId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE,
            grantedByUserId,
            Timestamp.from(now)
        );
        // Return the actual permission_id (could be existing on conflict)
        return findActivePermissionId(passportId, providerGroupId).orElse(permissionId);
    }

    @Override
    public void revokeConsentByPassportAndGroup(String passportId, String providerGroupId) {
        jdbcTemplate.update(
            """
                UPDATE passport_permissions
                SET status = ?
                WHERE passport_id = ?
                  AND seller_group_id = ?
                  AND scope = ?
                  AND status = ?
                  AND linked_service_request_id IS NULL
            """,
            STATUS_REVOKED,
            passportId,
            providerGroupId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE
        );
    }

    @Override
    public void linkServiceRequest(String permissionId, String serviceRequestId) {
        jdbcTemplate.update(
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
    public Optional<String> findActivePermissionId(String passportId, String providerGroupId) {
        List<String> ids = jdbcTemplate.queryForList(
            """
                SELECT permission_id FROM passport_permissions
                WHERE passport_id = ?
                  AND seller_group_id = ?
                  AND scope = ?
                  AND status = ?
            """,
            String.class,
            passportId,
            providerGroupId,
            SCOPE_SERVICE_REPAIR,
            STATUS_ACTIVE
        );
        return ids.stream().findFirst();
    }
}
