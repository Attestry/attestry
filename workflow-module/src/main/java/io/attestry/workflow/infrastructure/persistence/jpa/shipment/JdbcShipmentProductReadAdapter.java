package io.attestry.workflow.infrastructure.persistence.jpa.shipment;

import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
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

    private static final String PASSPORT_ASSET_STATE_ACTIVE = "ACTIVE";
    private static final String PASSPORT_RISK_FLAG_NONE = "NONE";
    private static final String SHIPMENT_STATUS_RELEASED = "RELEASED";

    private final JdbcTemplate jdbcTemplate;

    public JdbcShipmentProductReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PassportState> findPassportState(String passportId) {
        List<PassportState> rows = jdbcTemplate.query(
            """
                SELECT passport_id, tenant_id, asset_state, risk_flag
                FROM workflow_passport_state_projection
                WHERE passport_id = ?
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
                SELECT passport_id,
                       asset_id,
                       serial_number,
                       model_id,
                       model_name,
                       production_batch,
                       factory_code
                  FROM workflow_passport_catalog_projection
                 WHERE passport_id IN (%s)
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
                 WHERE wpsp.tenant_id = ?
                   AND wpsp.asset_state = ?
                   AND wpsp.risk_flag = ?
                   AND NOT EXISTS (
                        SELECT 1
                          FROM workflow_shipments ws
                         WHERE ws.passport_id = wpsp.passport_id
                           AND ws.status = ?
                   )
            """);
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        params.add(PASSPORT_ASSET_STATE_ACTIVE);
        params.add(PASSPORT_RISK_FLAG_NONE);
        params.add(SHIPMENT_STATUS_RELEASED);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(wpcp.serial_number) LIKE ? OR LOWER(wpcp.model_name) LIKE ?) ");
            params.add(like);
            params.add(like);
        }

        String fromClause = """
                  FROM workflow_passport_state_projection wpsp
                  JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = wpsp.passport_id
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
                SELECT wpcp.passport_id,
                       wpcp.asset_id,
                       wpcp.serial_number,
                       wpcp.model_id,
                       wpcp.model_name,
                       wpcp.production_batch,
                       wpcp.factory_code
            """ + fromClause + whereClause + """
                 ORDER BY wpcp.updated_at DESC, wpcp.passport_id DESC
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
    public PagedShipmentReadResult findShipmentsByTenantId(
            String tenantId, int page, int size, String keyword) {
        StringBuilder whereClause = new StringBuilder(" WHERE ws.tenant_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            whereClause.append(" AND (LOWER(wpcp.serial_number) LIKE ? OR LOWER(wpcp.model_name) LIKE ?) ");
            params.add(like);
            params.add(like);
        }

        String fromClause = """
                  FROM workflow_shipments ws
                  JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = ws.passport_id
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

        List<ShipmentJoinedRow> content = jdbcTemplate.query(
            """
                SELECT ws.shipment_id,
                       ws.tenant_id,
                       ws.passport_id,
                       wpcp.asset_id,
                       wpcp.serial_number,
                       wpcp.model_id,
                       wpcp.model_name,
                       wpcp.production_batch,
                       wpcp.factory_code,
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
                Timestamp releasedAt = rs.getTimestamp("released_at");
                return new ShipmentJoinedRow(
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
                    releasedAt != null ? releasedAt.toInstant() : null,
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

        return new PagedShipmentReadResult(content, page, size, total, totalPages);
    }
}
