package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.domain.permission.model.PermissionScope;
import io.attestry.product.domain.permission.model.PermissionStatus;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
    prefix = "app.product.passport-permission-projection",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DelegationPassportPermissionProjectionSync {

    private static final String PASSPORT_RESOURCE_TYPE = "PASSPORT";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DelegationPassportPermissionProjectionSync(
        JdbcTemplate jdbcTemplate,
        Clock clock,
        @Value("${app.product.passport-permission-projection.sync-interval-ms:5000}") long syncIntervalMs
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        if (syncIntervalMs < 1000) {
            throw new IllegalArgumentException("sync interval must be at least 1000ms");
        }
    }

    @Scheduled(
        fixedDelayString = "${app.product.passport-permission-projection.sync-interval-ms:5000}",
        initialDelayString = "${app.product.passport-permission-projection.initial-delay-ms:3000}"
    )
    @Transactional
    public void sync() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            Instant now = Instant.now(clock);
            List<DelegationRow> delegations = jdbcTemplate.query(
                """
                    SELECT d.delegation_id,
                           d.source_tenant_id,
                           d.target_tenant_id,
                           d.resource_id,
                           d.permission_code,
                           d.status AS delegation_status,
                           d.expires_at,
                           d.created_at,
                           COALESCE(pl.status, 'TERMINATED') AS partner_link_status
                    FROM delegations d
                    LEFT JOIN partner_links pl ON pl.partner_link_id = d.partner_link_id
                    WHERE d.resource_type = ?
                      AND d.permission_code = ?
                    """,
                (rs, rowNum) -> mapDelegation(rs),
                PASSPORT_RESOURCE_TYPE,
                PermissionCodes.RETAIL_TRANSFER_CREATE
            );
            for (DelegationRow delegation : delegations) {
                upsertProjection(delegation, now);
            }
        } finally {
            running.set(false);
        }
    }

    private void upsertProjection(DelegationRow delegation, Instant now) {
        if (!existsPassport(delegation.resourceId())) {
            return;
        }
        PermissionStatus projectedStatus = toProjectionStatus(delegation, now);
        jdbcTemplate.update(
            """
                INSERT INTO passport_permissions (
                    permission_id,
                    passport_id,
                    seller_group_id,
                    scope,
                    status,
                    expires_at,
                    created_at,
                    source_delegation_id,
                    source_tenant_id,
                    target_tenant_id,
                    resource_type,
                    resource_id,
                    permission_code,
                    last_synced_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_delegation_id) DO UPDATE
                SET passport_id = EXCLUDED.passport_id,
                    seller_group_id = EXCLUDED.seller_group_id,
                    scope = EXCLUDED.scope,
                    status = EXCLUDED.status,
                    expires_at = EXCLUDED.expires_at,
                    source_tenant_id = EXCLUDED.source_tenant_id,
                    target_tenant_id = EXCLUDED.target_tenant_id,
                    resource_type = EXCLUDED.resource_type,
                    resource_id = EXCLUDED.resource_id,
                    permission_code = EXCLUDED.permission_code,
                    last_synced_at = EXCLUDED.last_synced_at
                """,
            delegation.delegationId(),
            delegation.resourceId(),
            delegation.targetTenantId(),
            PermissionScope.RETAIL_SALE.name(),
            projectedStatus.name(),
            delegation.expiresAt() == null ? null : Timestamp.from(delegation.expiresAt()),
            Timestamp.from(delegation.createdAt()),
            delegation.delegationId(),
            delegation.sourceTenantId(),
            delegation.targetTenantId(),
            PASSPORT_RESOURCE_TYPE,
            delegation.resourceId(),
            delegation.permissionCode(),
            Timestamp.from(now)
        );
    }

    private PermissionStatus toProjectionStatus(DelegationRow delegation, Instant now) {
        if ("REVOKED".equals(delegation.delegationStatus())) {
            return PermissionStatus.REVOKED;
        }
        if ("EXPIRED".equals(delegation.delegationStatus())) {
            return PermissionStatus.EXPIRED;
        }
        if (delegation.expiresAt() != null && !delegation.expiresAt().isAfter(now)) {
            return PermissionStatus.EXPIRED;
        }
        if (!"ACTIVE".equals(delegation.partnerLinkStatus())) {
            return PermissionStatus.LINK_INACTIVE;
        }
        return PermissionStatus.ACTIVE;
    }

    private boolean existsPassport(String passportId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM product_passports WHERE passport_id = ?",
            Integer.class,
            passportId
        );
        return count != null && count > 0;
    }

    private DelegationRow mapDelegation(ResultSet rs) throws SQLException {
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new DelegationRow(
            rs.getString("delegation_id"),
            rs.getString("source_tenant_id"),
            rs.getString("target_tenant_id"),
            rs.getString("resource_id"),
            rs.getString("permission_code"),
            rs.getString("delegation_status"),
            rs.getString("partner_link_status"),
            expiresAt == null ? null : expiresAt.toInstant(),
            createdAt.toInstant()
        );
    }

    private record DelegationRow(
        String delegationId,
        String sourceTenantId,
        String targetTenantId,
        String resourceId,
        String permissionCode,
        String delegationStatus,
        String partnerLinkStatus,
        Instant expiresAt,
        Instant createdAt
    ) {
    }
}
