package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.CompletedTransferQueryPort;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCompletedTransferQueryAdapter implements CompletedTransferQueryPort {

    private static final String COMPLETED_B2C_WHERE = """
        WHERE tt.tenant_id = ?
          AND tt.passport_id = ?
          AND tt.transfer_type = 'B2C'
          AND tt.status = 'COMPLETED'
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCompletedTransferQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PagedResult findCompletedB2CByTenantId(String tenantId, String sourceTenantId, int page, int size) {
        StringBuilder filters = new StringBuilder("""
            WHERE tt.tenant_id = ?
              AND tt.transfer_type = 'B2C'
              AND tt.status = 'COMPLETED'
            """);
        List<Object> filterParams = new ArrayList<>();
        filterParams.add(tenantId);
        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            filters.append(" AND pp.tenant_id = ? ");
            filterParams.add(sourceTenantId);
        }

        Long totalElements = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM token_transfers tt
                JOIN product_passports pp ON pp.passport_id = tt.passport_id
            """
                + filters,
            Long.class,
            filterParams.toArray()
        );
        long total = totalElements != null ? totalElements : 0L;
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);

        List<Object> contentParams = new ArrayList<>(filterParams);
        contentParams.add(size);
        contentParams.add(page * size);

        List<CompletedTransferRow> content = jdbcTemplate.query(
            """
                SELECT tt.transfer_id,
                       tt.passport_id,
                       pp.tenant_id AS source_tenant_id,
                       pa.serial_number,
                       pa.model_name,
                       pa.asset_state,
                       tt.to_owner_id,
                       tt.accept_method,
                       tt.completed_at
                FROM token_transfers tt
                JOIN product_passports pp ON pp.passport_id = tt.passport_id
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
            """
                + filters
                + """
                ORDER BY tt.completed_at DESC, tt.created_at DESC
                LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new CompletedTransferRow(
                rs.getString("transfer_id"),
                rs.getString("passport_id"),
                rs.getString("source_tenant_id"),
                rs.getString("serial_number"),
                rs.getString("model_name"),
                rs.getString("asset_state"),
                rs.getString("to_owner_id"),
                rs.getString("accept_method"),
                toInstant(rs.getTimestamp("completed_at"))
            ),
            contentParams.toArray()
        );

        return new PagedResult(content, page, size, total, totalPages);
    }

    @Override
    public boolean existsCompletedB2CByTenantAndPassportId(String tenantId, String passportId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM token_transfers tt " + COMPLETED_B2C_WHERE,
            Long.class,
            tenantId,
            passportId
        );
        return count != null && count > 0;
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
