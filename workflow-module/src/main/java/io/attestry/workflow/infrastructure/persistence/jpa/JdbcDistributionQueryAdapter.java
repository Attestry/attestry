package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.DistributionQueryPort;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDistributionQueryAdapter implements DistributionQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDistributionQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SELECT_CLAUSE = """
            SELECT d.distribution_id,
                   d.passport_id,
                   d.source_tenant_id,
                   d.target_tenant_id,
                   t.name AS target_tenant_name,
                   t.type AS target_tenant_type,
                   d.partner_link_id,
                   d.delegation_id,
                   d.status,
                   pa.serial_number,
                   pa.model_name,
                   d.distributed_by_user_id,
                   d.distributed_at,
                   d.recalled_by_user_id,
                   d.recalled_at,
                   d.recall_reason
            """;

    private static final String FROM_CLAUSE = """
            FROM distributions d
            JOIN tenants t ON t.tenant_id = d.target_tenant_id
            JOIN product_passports pp ON pp.passport_id = d.passport_id
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
            """;

    private static final RowMapper<DistributionRow> ROW_MAPPER = (rs, rowNum) -> {
        Timestamp recalledAt = rs.getTimestamp("recalled_at");
        return new DistributionRow(
            rs.getString("distribution_id"),
            rs.getString("passport_id"),
            rs.getString("source_tenant_id"),
            rs.getString("target_tenant_id"),
            rs.getString("target_tenant_name"),
            rs.getString("target_tenant_type"),
            rs.getString("partner_link_id"),
            rs.getString("delegation_id"),
            rs.getString("status"),
            rs.getString("serial_number"),
            rs.getString("model_name"),
            rs.getString("distributed_by_user_id"),
            rs.getTimestamp("distributed_at").toInstant(),
            rs.getString("recalled_by_user_id"),
            recalledAt != null ? recalledAt.toInstant() : null,
            rs.getString("recall_reason")
        );
    };

    @Override
    public PagedDistributionResult findBySourceTenantId(String sourceTenantId, int page, int size, String keyword) {
        StringBuilder whereClause = new StringBuilder(" WHERE d.source_tenant_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(sourceTenantId);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(pa.serial_number) LIKE ? OR LOWER(pa.model_name) LIKE ?) ");
            params.add(like);
            params.add(like);
        }

        Long totalElements = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + FROM_CLAUSE + whereClause,
            Long.class,
            params.toArray()
        );
        long total = totalElements != null ? totalElements : 0;
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        List<Object> contentParams = new ArrayList<>(params);
        contentParams.add(size);
        contentParams.add(page * size);

        List<DistributionRow> content = jdbcTemplate.query(
            SELECT_CLAUSE + FROM_CLAUSE + whereClause + " ORDER BY d.distributed_at DESC LIMIT ? OFFSET ?",
            ROW_MAPPER,
            contentParams.toArray()
        );

        return new PagedDistributionResult(content, page, size, total, totalPages);
    }

    @Override
    public Optional<DistributionRow> findById(String distributionId) {
        return querySingle(" WHERE d.distribution_id = ? ", distributionId);
    }

    @Override
    public Optional<DistributionRow> findLatestByPassportId(String passportId) {
        return querySingle(
            " WHERE d.passport_id = ? ORDER BY d.distributed_at DESC LIMIT 1 ",
            passportId
        );
    }

    private Optional<DistributionRow> querySingle(String suffix, Object... args) {
        List<DistributionRow> results = jdbcTemplate.query(
            SELECT_CLAUSE + FROM_CLAUSE + suffix,
            ROW_MAPPER,
            args
        );
        return results.stream().findFirst();
    }
}
