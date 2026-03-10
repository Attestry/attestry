package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.application.port.DistributedPassportQueryPort;
import io.attestry.product.domain.ProductDomainException;
import io.attestry.product.domain.ProductErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDistributedPassportQueryAdapter implements DistributedPassportQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDistributedPassportQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PagedResult findByTargetTenant(String tenantId, int page, int size, String keyword, String sourceTenantId) {
        RowMapper<DistributedPassportView> rowMapper = (rs, rowNum) -> new DistributedPassportView(
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

        StringBuilder filters = new StringBuilder("""
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
            """);
        List<Object> filterParams = new ArrayList<>();
        filterParams.add(tenantId);
        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            filters.append(" AND ppm.source_tenant_id = ? ");
            filterParams.add(sourceTenantId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String keywordFilter = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            filters.append(" AND (LOWER(pa.serial_number) LIKE ? OR LOWER(pa.model_name) LIKE ?) ");
            filterParams.add(keywordFilter);
            filterParams.add(keywordFilter);
        }

        String baseSql = """
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
        long total = totalElements != null ? totalElements : 0;
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
            rowMapper,
            contentParams.toArray()
        );

        return new PagedResult(content, page, size, total, totalPages);
    }

    @Override
    public DistributedPassportDetailView findDetailByRetailAccess(String tenantId, String passportId) {
        RowMapper<DistributedPassportDetailView> rowMapper = (rs, rowNum) -> new DistributedPassportDetailView(
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
        );

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
              AND (
                  EXISTS (
                      SELECT 1
                      FROM passport_permissions ppm
                      WHERE ppm.passport_id = pp.passport_id
                        AND ppm.target_tenant_id = ?
                        AND ppm.status = 'ACTIVE'
                        AND ppm.permission_code = 'RETAIL_TRANSFER_CREATE'
                        AND ppm.resource_type = 'PASSPORT'
                        AND (ppm.expires_at IS NULL OR ppm.expires_at > CURRENT_TIMESTAMP)
                  )
                  OR EXISTS (
                      SELECT 1
                      FROM token_transfers tt
                      WHERE tt.passport_id = pp.passport_id
                        AND tt.tenant_id = ?
                        AND tt.transfer_type = 'B2C'
                        AND tt.status = 'COMPLETED'
                  )
              )
            """,
            rowMapper,
            passportId,
            tenantId,
            tenantId
        );

        if (results.isEmpty()) {
            throw new ProductDomainException(ProductErrorCode.ASSET_NOT_FOUND,
                "Distributed passport not found for tenant: " + passportId);
        }
        return results.get(0);
    }
}
