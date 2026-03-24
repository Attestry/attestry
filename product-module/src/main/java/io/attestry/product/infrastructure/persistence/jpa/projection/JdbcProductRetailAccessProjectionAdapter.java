package io.attestry.product.infrastructure.persistence.jpa.projection;

import io.attestry.product.application.query.view.DistributedPassportDetailView;
import io.attestry.product.application.query.view.DistributedPassportView;
import io.attestry.product.application.port.query.DistributedPassportQueryPort;
import io.attestry.product.application.port.projection.ProductRetailAccessProjectionPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcProductRetailAccessProjectionAdapter
    implements ProductRetailAccessProjectionPort, DistributedPassportQueryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private static final String DETAIL_SELECT_COLUMNS = """
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
        """;

    @Override
    public PagedRetailAccessResult findAccessiblePassports(
        String tenantId,
        int page,
        int size,
        String keyword,
        String sourceTenantId
    ) {
        StringBuilder whereClause = new StringBuilder("""
            WHERE prap.tenant_id = :tenantId
              AND prap.access_status = 'ACTIVE'
        """);
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId", tenantId);

        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            whereClause.append(" AND prap.source_tenant_id = :sourceTenantId ");
            params.put("sourceTenantId", sourceTenantId);
        }
        if (keyword != null && !keyword.isBlank()) {
            whereClause.append(" AND (LOWER(pa.serial_number) LIKE :keyword OR LOWER(pa.model_name) LIKE :keyword) ");
            params.put("keyword", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        }

        String fromClause = """
            FROM product_retail_access_projection prap
            JOIN product_passports pp ON pp.passport_id = prap.passport_id
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
        """;
        MapSqlParameterSource parameterSource = new MapSqlParameterSource(params);

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + fromClause + whereClause,
            parameterSource,
            Long.class
        );
        long totalElements = total == null ? 0L : total;
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        List<RetailAccessRow> content = jdbcTemplate.query(
            """
                SELECT prap.passport_id,
                       pp.qr_public_code,
                       pa.asset_id,
                       pa.serial_number,
                       pa.model_id,
                       pa.model_name,
                       pa.asset_state,
                       pa.risk_flag,
                       prap.access_source_type,
                       prap.access_source_id,
                       prap.expires_at,
                       prap.source_tenant_id,
                       prap.tenant_id,
                       prap.access_status,
                       prap.granted_at
            """ + fromClause + whereClause + " ORDER BY prap.granted_at DESC LIMIT :limit OFFSET :offset",
            parameterSource.addValue("limit", size).addValue("offset", page * size),
            (rs, rowNum) -> new RetailAccessRow(
                rs.getString("passport_id"),
                rs.getString("qr_public_code"),
                rs.getString("asset_id"),
                rs.getString("serial_number"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("asset_state"),
                rs.getString("risk_flag"),
                rs.getString("access_source_type"),
                rs.getString("access_source_id"),
                toInstant(rs.getTimestamp("expires_at")),
                rs.getString("source_tenant_id"),
                rs.getString("tenant_id"),
                rs.getString("access_status"),
                toInstant(rs.getTimestamp("granted_at"))
            )
        );

        return new PagedRetailAccessResult(content, page, size, totalElements, totalPages);
    }

    @Override
    public PagedResult findByTargetTenant(String tenantId, int page, int size, String keyword, String sourceTenantId) {
        PagedRetailAccessResult result = findAccessiblePassports(tenantId, page, size, keyword, sourceTenantId);
        List<DistributedPassportView> content = result.content().stream()
            .map(row -> new DistributedPassportView(
                row.passportId(),
                row.qrPublicCode(),
                row.assetId(),
                row.serialNumber(),
                row.modelId(),
                row.modelName(),
                row.assetState(),
                row.riskFlag(),
                row.accessSourceType().equals("PERMISSION") ? row.accessSourceId() : null,
                row.expiresAt(),
                row.sourceTenantId(),
                row.targetTenantId(),
                row.accessStatus(),
                row.grantedAt()
            ))
            .toList();
        return new PagedResult(content, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Override
    public Optional<RetailAccessDetailView> findAccessiblePassportDetail(String tenantId, String passportId) {
        List<RetailAccessDetailView> rows = jdbcTemplate.query(
            DETAIL_SELECT_COLUMNS
                + """
                    ,
                       prap.access_source_type,
                       prap.access_source_id,
                       prap.updated_at
                    FROM product_retail_access_projection prap
                    JOIN product_passports pp ON pp.passport_id = prap.passport_id
                    JOIN product_assets pa ON pa.asset_id = pp.asset_id
                    WHERE prap.tenant_id = :tenantId
                      AND prap.passport_id = :passportId
                      AND prap.access_status = 'ACTIVE'
                    ORDER BY prap.granted_at DESC
                    LIMIT 1
                """,
            new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("passportId", passportId),
            (rs, rowNum) -> new RetailAccessDetailView(
                rs.getString("passport_id"),
                rs.getString("qr_public_code"),
                rs.getString("serial_number"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("asset_state"),
                rs.getString("risk_flag"),
                toInstant(rs.getTimestamp("manufactured_at")),
                rs.getString("production_batch"),
                rs.getString("factory_code"),
                rs.getString("access_source_type"),
                rs.getString("access_source_id"),
                toInstant(rs.getTimestamp("updated_at"))
            )
        );

        return rows.stream().findFirst();
    }

    @Override
    public DistributedPassportDetailView findDetailByRetailAccess(String tenantId, String passportId) {
        return findAccessiblePassportDetail(tenantId, passportId)
            .map(this::toDistributedPassportDetailView)
            .orElseThrow(() -> new ProductDomainException(
                ProductErrorCode.ASSET_NOT_FOUND,
                "Distributed passport not found for tenant: " + passportId
            ));
    }

    @Override
    public DistributedPassportDetailView findDetailByCompletedTransfer(String tenantId, String passportId) {
        List<DistributedPassportDetailView> rows = jdbcTemplate.query(
            DETAIL_SELECT_COLUMNS
                + """
                    FROM token_transfers tt
                    JOIN product_passports pp ON pp.passport_id = tt.passport_id
                    JOIN product_assets pa ON pa.asset_id = pp.asset_id
                    WHERE tt.tenant_id = :tenantId
                      AND tt.passport_id = :passportId
                      AND tt.transfer_type = 'B2C'
                      AND tt.status = 'COMPLETED'
                    ORDER BY tt.completed_at DESC, tt.created_at DESC
                    LIMIT 1
                """,
            new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("passportId", passportId),
            (rs, rowNum) -> mapDistributedPassportDetailView(rs)
        );

        return rows.stream().findFirst()
            .orElseThrow(() -> new ProductDomainException(
                ProductErrorCode.ASSET_NOT_FOUND,
                "Completed transfer passport not found for tenant: " + passportId
            ));
    }

    private DistributedPassportDetailView toDistributedPassportDetailView(RetailAccessDetailView row) {
        return new DistributedPassportDetailView(
            row.passportId(),
            row.qrPublicCode(),
            row.serialNumber(),
            row.modelId(),
            row.modelName(),
            row.assetState(),
            row.riskFlag(),
            row.manufacturedAt(),
            row.productionBatch(),
            row.factoryCode()
        );
    }

    private DistributedPassportDetailView mapDistributedPassportDetailView(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DistributedPassportDetailView(
            rs.getString("passport_id"),
            rs.getString("qr_public_code"),
            rs.getString("serial_number"),
            rs.getString("model_id"),
            rs.getString("model_name"),
            rs.getString("asset_state"),
            rs.getString("risk_flag"),
            toInstant(rs.getTimestamp("manufactured_at")),
            rs.getString("production_batch"),
            rs.getString("factory_code")
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
