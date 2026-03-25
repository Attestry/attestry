package io.attestry.workflow.infrastructure.persistence.jdbc.projection;

import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.delegation.DelegationPermissionProjectionPort;
import io.attestry.workflow.application.port.projection.WorkflowPassportProjectionWritePort;
import io.attestry.workflow.domain.delegation.model.Delegation;
import io.attestry.workflow.domain.delegation.model.DelegationStatus;
import io.attestry.workflow.domain.partner.model.PartnerLinkStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDelegationPermissionProjectionAdapter implements DelegationPermissionProjectionPort {

    private static final String PASSPORT_RESOURCE_TYPE = "PASSPORT";
    private static final String PASSPORT_PERMISSION_SCOPE_RETAIL_SALE = "RETAIL_SALE";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_LINK_INACTIVE = "LINK_INACTIVE";
    private static final String STATUS_CONSUMED = "CONSUMED";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final WorkflowPassportProjectionWritePort projectionWriter;

    public JdbcDelegationPermissionProjectionAdapter(
        NamedParameterJdbcTemplate jdbcTemplate,
        Clock clock,
        WorkflowPassportProjectionWritePort projectionWriter
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.projectionWriter = projectionWriter;
    }

    @Override
    public void onDelegationGranted(Delegation delegation, String partnerLinkStatus) {
        if (!isSupportedPassportDelegation(delegation)) {
            return;
        }
        String status = resolveStatus(delegation, partnerLinkStatus, Instant.now(clock));
        upsert(delegation, status);
    }

    @Override
    public void onDelegationRevoked(Delegation delegation) {
        if (!isSupportedPassportDelegation(delegation)) {
            return;
        }
        upsert(delegation, STATUS_REVOKED);
    }

    @Override
    public void onDelegationExpired(Delegation delegation) {
        if (!isSupportedPassportDelegation(delegation)) {
            return;
        }
        upsert(delegation, STATUS_EXPIRED);
    }

    @Override
    public void onDelegationConsumed(Delegation delegation) {
        if (!isSupportedPassportDelegation(delegation)) {
            return;
        }
        upsert(delegation, STATUS_CONSUMED);
    }

    private boolean isSupportedPassportDelegation(Delegation delegation) {
        return PASSPORT_RESOURCE_TYPE.equals(delegation.resourceType())
            && PermissionCodes.RETAIL_TRANSFER_CREATE.equals(delegation.permissionCode());
    }

    private String resolveStatus(Delegation delegation, String partnerLinkStatus, Instant now) {
        if (delegation.status() == DelegationStatus.REVOKED) {
            return STATUS_REVOKED;
        }
        if (delegation.expiresAt() != null && !delegation.expiresAt().isAfter(now)) {
            return STATUS_EXPIRED;
        }
        if (PartnerLinkStatus.valueOf(partnerLinkStatus) != PartnerLinkStatus.ACTIVE) {
            return STATUS_LINK_INACTIVE;
        }
        return STATUS_ACTIVE;
    }

    private void upsert(Delegation delegation, String status) {
        Instant now = Instant.now(clock);
        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO passport_permissions (
                    permission_id,
                    passport_id,
                    seller_tenant_id,
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
                    seller_tenant_id = EXCLUDED.seller_tenant_id,
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
            PASSPORT_PERMISSION_SCOPE_RETAIL_SALE,
            status,
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

        projectionWriter.syncPermissionById(
            delegation.delegationId(),
            "delegation-permission:" + delegation.delegationId() + ":" + status,
            null,
            now
        );
    }
}
