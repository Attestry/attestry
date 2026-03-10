package io.attestry.adapters.product;

import io.attestry.product.application.dto.command.ProductActor;
import io.attestry.product.application.dto.command.ProductTenantType;
import io.attestry.product.application.port.DistributedPassportQueryPort;
import io.attestry.product.application.port.PassportDistributionQueryPort;
import io.attestry.product.application.port.ProductAuthorizationPort;
import io.attestry.product.application.port.TenantContextAccessPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.application.dto.command.AuthzEvaluateCommand;
import io.attestry.userauth.application.dto.command.PolicyDecisionMode;
import io.attestry.userauth.application.dto.result.AuthzEvaluateResult;
import io.attestry.userauth.application.port.MembershipPort;
import io.attestry.userauth.application.usecase.policy.EvaluateAuthorizationUseCase;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.workflow.application.port.CompletedTransferQueryPort;
import io.attestry.workflow.application.port.DistributionQueryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class ProductAdapter implements
    ProductAuthorizationPort,
    TenantContextAccessPort,
    DistributedPassportQueryPort,
    PassportDistributionQueryPort {

    private static final String ACTIVE_RETAIL_PERMISSION_FILTER = """
        WHERE ppm.target_tenant_id = ?
          AND ppm.status = 'ACTIVE'
          AND ppm.permission_code = 'RETAIL_TRANSFER_CREATE'
          AND ppm.resource_type = 'PASSPORT'
          AND (ppm.expires_at IS NULL OR ppm.expires_at > CURRENT_TIMESTAMP)
          AND NOT EXISTS (
              SELECT 1
              FROM token_transfers tt
              WHERE tt.passport_id = ppm.passport_id
                AND tt.tenant_id = ppm.target_tenant_id
                AND tt.transfer_type = 'B2C'
                AND tt.status = 'COMPLETED'
          )
        """;

    private final EvaluateAuthorizationUseCase evaluateAuthorizationUseCase;
    private final MembershipPort membershipPort;
    private final JdbcTemplate jdbcTemplate;
    private final DistributionQueryPort distributionQueryPort;
    private final CompletedTransferQueryPort completedTransferQueryPort;

    public ProductAdapter(
        EvaluateAuthorizationUseCase evaluateAuthorizationUseCase,
        MembershipPort membershipPort,
        JdbcTemplate jdbcTemplate,
        DistributionQueryPort distributionQueryPort,
        CompletedTransferQueryPort completedTransferQueryPort
    ) {
        this.evaluateAuthorizationUseCase = evaluateAuthorizationUseCase;
        this.membershipPort = membershipPort;
        this.jdbcTemplate = jdbcTemplate;
        this.distributionQueryPort = distributionQueryPort;
        this.completedTransferQueryPort = completedTransferQueryPort;
    }

    @Override
    public void assertBrandMintAllowed(ProductActor actor, String tenantId, String serialNumber) {
        assertAllowed(
            actor,
            tenantId,
            PermissionCodes.BRAND_MINT,
            "mint:" + tenantId + ":" + serialNumber,
            PolicyDecisionMode.LIVE_RECHECK,
            ProductErrorCode.FORBIDDEN_MINT,
            "BRAND_MINT scope is required"
        );
    }

    @Override
    public void assertBrandVoidAllowed(ProductActor actor, String tenantId, String passportId) {
        assertAllowed(
            actor,
            tenantId,
            PermissionCodes.BRAND_VOID,
            "void:" + passportId,
            PolicyDecisionMode.LIVE_RECHECK,
            ProductErrorCode.FORBIDDEN_VOID,
            "BRAND_VOID scope is required"
        );
    }

    @Override
    public void assertOwnerRiskFlagAllowed(ProductActor actor) {
        assertAllowed(
            actor,
            actor.tenantId(),
            PermissionCodes.OWNER_RISK_FLAG,
            null,
            PolicyDecisionMode.TOKEN_SNAPSHOT,
            ProductErrorCode.FORBIDDEN_RISK_FLAG,
            "OWNER_RISK_FLAG scope is required"
        );
    }

    @Override
    public void assertOwnerRiskClearAllowed(ProductActor actor) {
        assertAllowed(
            actor,
            actor.tenantId(),
            PermissionCodes.OWNER_RISK_CLEAR,
            null,
            PolicyDecisionMode.TOKEN_SNAPSHOT,
            ProductErrorCode.FORBIDDEN_RISK_FLAG,
            "OWNER_RISK_CLEAR scope is required"
        );
    }

    @Override
    public void assertPassportPermissionGrantAllowed(ProductActor actor) {
        assertAllowed(
            actor,
            actor.tenantId(),
            PermissionCodes.PASSPORT_PERMISSION_GRANT,
            null,
            PolicyDecisionMode.LIVE_RECHECK,
            ProductErrorCode.FORBIDDEN_VOID,
            "PASSPORT_PERMISSION_GRANT scope is required"
        );
    }

    @Override
    public void assertActiveTenantMembership(String actorUserId, String tenantId, ProductTenantType tenantType) {
        Membership membership = membershipPort.findByUserIdAndTenantId(actorUserId, tenantId)
            .orElseThrow(() -> missingTenantContext(tenantType));

        if (!membership.isActive() || !membership.groupType().name().equals(tenantType.name())) {
            throw missingTenantContext(tenantType);
        }
    }

    @Override
    public PagedResult findByTargetTenant(String tenantId, int page, int size, String keyword, String sourceTenantId) {
        String filters = buildActiveRetailPermissionFilters(keyword, sourceTenantId);
        List<Object> filterParams = buildActiveRetailPermissionParams(tenantId, keyword, sourceTenantId);
        String baseSql = buildRankedRetailPermissionSql(filters);

        long total = queryRetailPermissionCount(baseSql, filterParams);
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);

        List<Object> contentParams = new ArrayList<>(filterParams);
        contentParams.add(size);
        contentParams.add(page * size);

        List<DistributedPassportView> content = jdbcTemplate.query(
            baseSql
                + """
                SELECT rp.passport_id,
                       pp.qr_public_code,
                       pa.asset_id,
                       pa.serial_number,
                       pa.model_id,
                       pa.model_name,
                       pa.asset_state,
                       pa.risk_flag,
                       rp.permission_id,
                       rp.expires_at,
                       rp.source_tenant_id,
                       rp.target_tenant_id,
                       rp.permission_status,
                       rp.created_at
                FROM ranked_permissions rp
                JOIN product_passports pp ON pp.passport_id = rp.passport_id
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                WHERE rp.rn = 1
                ORDER BY rp.created_at DESC
                LIMIT ? OFFSET ?
                """,
            distributedPassportRowMapper(),
            contentParams.toArray()
        );

        return new PagedResult(content, page, size, total, totalPages);
    }

    @Override
    public DistributedPassportDetailView findDetailByRetailAccess(String tenantId, String passportId) {
        assertRetailAccess(tenantId, passportId);

        List<DistributedPassportDetailView> results = jdbcTemplate.query(
            """
            SELECT pp.passport_id,
                   pp.qr_public_code,
                   pa.serial_number,
                   pa.model_id,
                   pa.model_name,
                   pa.asset_state,
                   pa.risk_flag,
                   pa.manufactured_at,
                   pa.production_batch,
                   pa.factory_code
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
            WHERE pp.passport_id = ?
            """,
            (rs, rowNum) -> new DistributedPassportDetailView(
                rs.getString("passport_id"),
                rs.getString("qr_public_code"),
                rs.getString("serial_number"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("asset_state"),
                rs.getString("risk_flag"),
                rs.getTimestamp("manufactured_at") == null ? null : rs.getTimestamp("manufactured_at").toInstant(),
                rs.getString("production_batch"),
                rs.getString("factory_code")
            ),
            passportId
        );

        if (results.isEmpty()) {
            throw new ProductDomainException(
                ProductErrorCode.ASSET_NOT_FOUND,
                "Distributed passport not found for tenant: " + passportId
            );
        }
        return results.get(0);
    }

    @Override
    public java.util.Optional<DistributionView> findLatestDistribution(String passportId) {
        return distributionQueryPort.findLatestByPassportId(passportId)
            .map(row -> new DistributionView(
                row.distributionId(),
                row.targetTenantId(),
                row.targetTenantName(),
                row.targetTenantType(),
                row.partnerLinkId(),
                row.status(),
                row.distributedAt()
            ));
    }

    private void assertRetailAccess(String tenantId, String passportId) {
        if (hasActiveRetailPermission(tenantId, passportId)) {
            return;
        }
        if (completedTransferQueryPort.existsCompletedB2CByTenantAndPassportId(tenantId, passportId)) {
            return;
        }
        throw new ProductDomainException(
            ProductErrorCode.ASSET_NOT_FOUND,
            "Distributed passport not found for tenant: " + passportId
        );
    }

    private boolean hasActiveRetailPermission(String tenantId, String passportId) {
        Long count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM passport_permissions ppm
            WHERE ppm.passport_id = ?
              AND ppm.target_tenant_id = ?
              AND ppm.status = 'ACTIVE'
              AND ppm.permission_code = 'RETAIL_TRANSFER_CREATE'
              AND ppm.resource_type = 'PASSPORT'
              AND (ppm.expires_at IS NULL OR ppm.expires_at > CURRENT_TIMESTAMP)
            """,
            Long.class,
            passportId,
            tenantId
        );
        return count != null && count > 0;
    }

    private ProductDomainException missingTenantContext(ProductTenantType tenantType) {
        return new ProductDomainException(
            ProductErrorCode.MINT_CONTEXT_NOT_FOUND,
            "Active " + tenantType.name() + " membership context is required"
        );
    }

    private String buildActiveRetailPermissionFilters(String keyword, String sourceTenantId) {
        StringBuilder filters = new StringBuilder(ACTIVE_RETAIL_PERMISSION_FILTER);
        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            filters.append(" AND ppm.source_tenant_id = ? ");
        }
        if (keyword != null && !keyword.isBlank()) {
            filters.append(" AND (LOWER(pa.serial_number) LIKE ? OR LOWER(pa.model_name) LIKE ?) ");
        }
        return filters.toString();
    }

    private List<Object> buildActiveRetailPermissionParams(String tenantId, String keyword, String sourceTenantId) {
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            params.add(sourceTenantId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String keywordFilter = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            params.add(keywordFilter);
            params.add(keywordFilter);
        }
        return params;
    }

    private String buildRankedRetailPermissionSql(String filters) {
        return """
            WITH ranked_permissions AS (
                SELECT ppm.permission_id,
                       ppm.passport_id,
                       ppm.expires_at,
                       ppm.source_tenant_id,
                       ppm.target_tenant_id,
                       ppm.status AS permission_status,
                       ppm.created_at,
                       ROW_NUMBER() OVER (
                           PARTITION BY ppm.passport_id
                           ORDER BY ppm.created_at DESC, ppm.permission_id DESC
                       ) AS rn
                FROM passport_permissions ppm
                JOIN product_passports pp ON pp.passport_id = ppm.passport_id
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
            """
            + filters
            + """
            )
            """;
    }

    private long queryRetailPermissionCount(String baseSql, List<Object> filterParams) {
        Long totalElements = jdbcTemplate.queryForObject(
            baseSql
                + """
                SELECT COUNT(*)
                FROM ranked_permissions
                WHERE rn = 1
                """,
            Long.class,
            filterParams.toArray()
        );
        return totalElements != null ? totalElements : 0L;
    }

    private RowMapper<DistributedPassportView> distributedPassportRowMapper() {
        return (rs, rowNum) -> new DistributedPassportView(
            rs.getString("passport_id"),
            rs.getString("qr_public_code"),
            rs.getString("asset_id"),
            rs.getString("serial_number"),
            rs.getString("model_id"),
            rs.getString("model_name"),
            rs.getString("asset_state"),
            rs.getString("risk_flag"),
            rs.getString("permission_id"),
            rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toInstant(),
            rs.getString("source_tenant_id"),
            rs.getString("target_tenant_id"),
            rs.getString("permission_status"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private void assertAllowed(
        ProductActor actor,
        String tenantId,
        String permissionCode,
        String resourceRef,
        PolicyDecisionMode decisionMode,
        ProductErrorCode errorCode,
        String message
    ) {
        AuthzEvaluateResult decision = evaluateAuthorizationUseCase.evaluate(
            toActorContext(actor),
            new AuthzEvaluateCommand(tenantId, permissionCode, resourceRef, decisionMode)
        );
        if (!decision.allowed()) {
            throw new ProductDomainException(errorCode, message);
        }
    }

    private ActorContext toActorContext(ProductActor actor) {
        return new ActorContext(
            null,
            actor.userId(),
            actor.tenantId(),
            null,
            actor.scopes() == null ? Set.of() : actor.scopes(),
            null
        );
    }
}
