package io.attestry.workflow.infrastructure.persistence.jpa.distribution;

import io.attestry.workflow.application.port.distribution.DistributionCandidateQueryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDistributionCandidateQueryAdapter implements DistributionCandidateQueryPort {

    private static final String SHIPMENT_STATUS_RELEASED = "RELEASED";
    private static final String DISTRIBUTION_STATUS_DISTRIBUTED = "DISTRIBUTED";

    private final JdbcTemplate jdbcTemplate;

    public JdbcDistributionCandidateQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PagedDistributionCandidateResult findDistributionCandidatesByTenantId(
        String tenantId, int page, int size, String keyword
    ) {
        String fromClause = """
              FROM workflow_shipments ws
              JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = ws.passport_id
            """;

        StringBuilder whereClause = new StringBuilder("""
             WHERE ws.tenant_id = ?
               AND ws.status = ?
               AND NOT EXISTS (
                    SELECT 1 FROM distributions d
                    WHERE d.passport_id = ws.passport_id
                      AND d.status = ?
               )
            """);

        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        params.add(SHIPMENT_STATUS_RELEASED);
        params.add(DISTRIBUTION_STATUS_DISTRIBUTED);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(wpcp.serial_number) LIKE ? OR LOWER(wpcp.model_name) LIKE ?) ");
            params.add(like);
            params.add(like);
        }

        Long totalElements = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + fromClause + whereClause,
            Long.class,
            params.toArray()
        );
        long total = totalElements != null ? totalElements : 0;
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        List<Object> contentParams = new ArrayList<>(params);
        contentParams.add(size);
        contentParams.add(page * size);

        List<DistributionCandidate> content = jdbcTemplate.query(
            """
                SELECT wpcp.passport_id,
                       wpcp.asset_id,
                       wpcp.serial_number,
                       wpcp.model_id,
                       wpcp.model_name,
                       wpcp.production_batch,
                       wpcp.factory_code
            """ + fromClause + whereClause + """
                 ORDER BY ws.released_at DESC
                 LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new DistributionCandidate(
                rs.getString("passport_id"),
                rs.getString("asset_id"),
                rs.getString("serial_number"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("production_batch"),
                rs.getString("factory_code")
            ),
            contentParams.toArray()
        );

        return new PagedDistributionCandidateResult(content, page, size, total, totalPages);
    }
}
