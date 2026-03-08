package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.ShipmentProductReadPort;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcShipmentProductReadAdapter implements ShipmentProductReadPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcShipmentProductReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PassportState> findPassportState(String passportId) {
        List<PassportState> rows = jdbcTemplate.query(
            """
                SELECT pp.passport_id, pp.tenant_id, pa.asset_state, pa.risk_flag
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                WHERE pp.passport_id = ?
            """,
            (rs, rowNum) -> new PassportState(
                rs.getString("passport_id"),
                rs.getString("tenant_id"),
                rs.getString("asset_state"),
                rs.getString("risk_flag")
            ),
            passportId
        );
        return rows.stream().findFirst();
    }

    @Override
    public Map<String, PassportAssetInfo> findPassportAssetInfoByIds(List<String> passportIds) {
        if (passportIds == null || passportIds.isEmpty()) return Collections.emptyMap();
        String inClause = passportIds.stream().map(id -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
            """
                SELECT pp.passport_id,
                       pa.asset_id,
                       pa.serial_number,
                       pa.model_id,
                       pa.model_name,
                       pa.production_batch,
                       pa.factory_code
                  FROM product_passports pp
                  JOIN product_assets pa ON pa.asset_id = pp.asset_id
                 WHERE pp.passport_id IN (%s)
            """.formatted(inClause),
            (rs, rowNum) -> new PassportAssetInfo(
                rs.getString("passport_id"),
                rs.getString("asset_id"),
                rs.getString("serial_number"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("production_batch"),
                rs.getString("factory_code")
            ),
            passportIds.toArray()
        ).stream().collect(Collectors.toMap(PassportAssetInfo::passportId, Function.identity()));
    }

    @Override
    public PagedReleaseCandidateResult findReleaseCandidatesByTenantId(
            String tenantId, int page, int size, String keyword) {
        StringBuilder whereClause = new StringBuilder("""
                 WHERE pp.tenant_id = ?
                   AND pa.asset_state = 'ACTIVE'
                   AND pa.risk_flag = 'NONE'
                   AND NOT EXISTS (
                        SELECT 1
                          FROM workflow_shipments ws
                         WHERE ws.passport_id = pp.passport_id
                           AND ws.status = 'RELEASED'
                   )
            """);
        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(pa.serial_number) LIKE ? OR LOWER(pa.model_name) LIKE ?) ");
            params.add(like);
            params.add(like);
        }

        String fromClause = """
                  FROM product_passports pp
                  JOIN product_assets pa ON pa.asset_id = pp.asset_id
            """;

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

        List<ShipmentReleaseCandidate> content = jdbcTemplate.query(
            """
                SELECT pp.passport_id,
                       pa.asset_id,
                       pa.serial_number,
                       pa.model_id,
                       pa.model_name,
                       pa.production_batch,
                       pa.factory_code
            """ + fromClause + whereClause + """
                 ORDER BY pp.created_at DESC
                 LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> new ShipmentReleaseCandidate(
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

        return new PagedReleaseCandidateResult(content, page, size, total, totalPages);
    }

    @Override
    public PagedShipmentViewResult findShipmentsByTenantId(
            String tenantId, int page, int size, String keyword) {
        StringBuilder whereClause = new StringBuilder(" WHERE ws.tenant_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(pa.serial_number) LIKE ? OR LOWER(pa.model_name) LIKE ?) ");
            params.add(like);
            params.add(like);
        }

        String fromClause = """
                  FROM workflow_shipments ws
                  JOIN product_passports pp ON pp.passport_id = ws.passport_id
                  JOIN product_assets pa ON pa.asset_id = pp.asset_id
            """;

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

        List<ShipmentJoinedView> content = jdbcTemplate.query(
            """
                SELECT ws.shipment_id,
                       ws.tenant_id,
                       ws.passport_id,
                       pa.asset_id,
                       pa.serial_number,
                       pa.model_id,
                       pa.model_name,
                       pa.production_batch,
                       pa.factory_code,
                       ws.shipment_round,
                       ws.status,
                       ws.released_at,
                       ws.released_by_user_id,
                       ws.released_by_tenant_id,
                       ws.evidence_group_id,
                       ws.returned_at,
                       ws.returned_by_user_id,
                       ws.return_evidence_group_id,
                       ws.created_at
            """ + fromClause + whereClause + """
                 ORDER BY ws.created_at DESC
                 LIMIT ? OFFSET ?
            """,
            (rs, rowNum) -> {
                Timestamp returnedAt = rs.getTimestamp("returned_at");
                return new ShipmentJoinedView(
                    rs.getString("shipment_id"),
                    rs.getString("tenant_id"),
                    rs.getString("passport_id"),
                    rs.getString("asset_id"),
                    rs.getString("serial_number"),
                    rs.getString("model_id"),
                    rs.getString("model_name"),
                    rs.getString("production_batch"),
                    rs.getString("factory_code"),
                    rs.getInt("shipment_round"),
                    rs.getString("status"),
                    rs.getTimestamp("released_at").toInstant(),
                    rs.getString("released_by_user_id"),
                    rs.getString("released_by_tenant_id"),
                    rs.getString("evidence_group_id"),
                    returnedAt != null ? returnedAt.toInstant() : null,
                    rs.getString("returned_by_user_id"),
                    rs.getString("return_evidence_group_id"),
                    rs.getTimestamp("created_at").toInstant()
                );
            },
            contentParams.toArray()
        );

        return new PagedShipmentViewResult(content, page, size, total, totalPages);
    }
}
