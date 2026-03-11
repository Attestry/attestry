package io.attestry.workflow.infrastructure.persistence.jpa.transfer;

import io.attestry.workflow.application.port.transfer.CompletedTransferQueryPort;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCompletedTransferQueryAdapter implements CompletedTransferQueryPort {

    private static final String TRANSFER_TYPE_B2C = "B2C";
    private static final String TRANSFER_STATUS_COMPLETED = "COMPLETED";

    private static final String COMPLETED_B2C_WHERE = """
        WHERE tt.tenant_id = ?
          AND tt.passport_id = ?
          AND tt.transfer_type = ?
          AND tt.status = ?
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCompletedTransferQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PagedResult findCompletedB2CByTenantId(String tenantId, String sourceTenantId, int page, int size) {
        StringBuilder filters = new StringBuilder("""
            WHERE tt.tenant_id = ?
              AND tt.transfer_type = ?
              AND tt.status = ?
            """);
        List<Object> filterParams = new ArrayList<>();
        filterParams.add(tenantId);
        filterParams.add(TRANSFER_TYPE_B2C);
        filterParams.add(TRANSFER_STATUS_COMPLETED);
        if (sourceTenantId != null && !sourceTenantId.isBlank()) {
            filters.append(" AND wpsp.tenant_id = ? ");
            filterParams.add(sourceTenantId);
        }

        Long totalElements = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM token_transfers tt
                JOIN workflow_passport_state_projection wpsp ON wpsp.passport_id = tt.passport_id
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
                       wpsp.tenant_id AS source_tenant_id,
                       wpcp.serial_number,
                       wpcp.model_name,
                       wpsp.asset_state,
                       tt.to_owner_id,
                       tt.accept_method,
                       tt.completed_at
                FROM token_transfers tt
                JOIN workflow_passport_state_projection wpsp ON wpsp.passport_id = tt.passport_id
                JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = tt.passport_id
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
            passportId,
            TRANSFER_TYPE_B2C,
            TRANSFER_STATUS_COMPLETED
        );
        return count != null && count > 0;
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
