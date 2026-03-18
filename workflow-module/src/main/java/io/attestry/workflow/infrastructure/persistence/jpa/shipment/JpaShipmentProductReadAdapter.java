package io.attestry.workflow.infrastructure.persistence.jpa.shipment;

import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaShipmentProductReadAdapter implements ShipmentProductReadPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Optional<PassportState> findPassportState(String passportId) {
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT passport_id,
                       tenant_id,
                       asset_state,
                       risk_flag
                FROM workflow_passport_state_projection
                WHERE passport_id = ?
            """,
            rs -> rs.next()
                ? Optional.of(new PassportState(
                    rs.getString("passport_id"),
                    rs.getString("tenant_id"),
                    rs.getString("asset_state"),
                    rs.getString("risk_flag")
                ))
                : Optional.empty(),
            passportId
        );
    }

    @Override
    public Map<String, PassportAssetInfo> findPassportAssetInfoByIds(List<String> passportIds) {
        if (passportIds == null || passportIds.isEmpty()) return Collections.emptyMap();
        return namedParameterJdbcTemplate.query(
            """
                SELECT passport_id,
                       asset_id,
                       serial_number,
                       model_id,
                       model_name,
                       production_batch,
                       factory_code
                FROM workflow_passport_catalog_projection
                WHERE passport_id IN (:passportIds)
            """,
            new MapSqlParameterSource("passportIds", passportIds),
            (rs, rowNum) -> new PassportAssetInfo(
                rs.getString("passport_id"),
                rs.getString("asset_id"),
                rs.getString("serial_number"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("production_batch"),
                rs.getString("factory_code")
            )
        ).stream()
            .collect(Collectors.toMap(PassportAssetInfo::passportId, Function.identity()));
    }

    @Override
    public PagedReleaseCandidateResult findReleaseCandidatesByTenantId(
        String tenantId, int page, int size, String keyword
    ) {
        StringBuilder whereClause = new StringBuilder("""
            WHERE wpsp.tenant_id = :tenantId
              AND wpsp.asset_state = 'ACTIVE'
              AND wpsp.risk_flag = 'NONE'
              AND NOT EXISTS (
                   SELECT 1 FROM shipments ws
                   WHERE ws.passport_id = wpsp.passport_id AND ws.status = 'RELEASED'
              )
        """);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("tenantId", tenantId);

        if (keyword != null && !keyword.isBlank()) {
            whereClause.append("""
                 AND (
                     LOWER(wpcp.passport_id) LIKE :keyword
                     OR LOWER(wpcp.serial_number) LIKE :keyword
                     OR LOWER(wpcp.model_name) LIKE :keyword
                 )
            """);
            params.addValue("keyword", "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%");
        }

        String fromClause = """
            FROM workflow_passport_state_projection wpsp
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = wpsp.passport_id
        """;

        Long total = namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + fromClause + whereClause,
            params,
            Long.class
        );
        long totalElements = total == null ? 0L : total;
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        List<ShipmentReleaseCandidate> content = namedParameterJdbcTemplate.query(
            """
                SELECT wpcp.passport_id,
                       wpcp.asset_id,
                       wpcp.serial_number,
                       wpcp.model_id,
                       wpcp.model_name,
                       wpcp.production_batch,
                       wpcp.factory_code
            """ + fromClause + whereClause + " ORDER BY wpcp.updated_at DESC, wpcp.passport_id DESC LIMIT :limit OFFSET :offset",
            params.addValue("limit", size).addValue("offset", page * size),
            (rs, rowNum) -> new ShipmentReleaseCandidate(
                rs.getString("passport_id"),
                rs.getString("asset_id"),
                rs.getString("serial_number"),
                rs.getString("model_id"),
                rs.getString("model_name"),
                rs.getString("production_batch"),
                rs.getString("factory_code")
            )
        );

        return new PagedReleaseCandidateResult(content, page, size, totalElements, totalPages);
    }

    @Override
    public PagedShipmentReadResult findShipmentsByTenantId(
        String tenantId, int page, int size, String keyword
    ) {
        StringBuilder whereClause = new StringBuilder(" WHERE ws.tenant_id = :tenantId ");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("tenantId", tenantId);

        if (keyword != null && !keyword.isBlank()) {
            whereClause.append("""
                 AND (
                     LOWER(wpcp.passport_id) LIKE :keyword
                     OR LOWER(wpcp.serial_number) LIKE :keyword
                     OR LOWER(wpcp.model_name) LIKE :keyword
                 )
            """);
            params.addValue("keyword", "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%");
        }

        String fromClause = """
            FROM shipments ws
            JOIN workflow_passport_catalog_projection wpcp ON wpcp.passport_id = ws.passport_id
        """;

        Long total = namedParameterJdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + fromClause + whereClause,
            params,
            Long.class
        );
        long totalElements = total == null ? 0L : total;
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        List<ShipmentJoinedRow> content = namedParameterJdbcTemplate.query(
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
            """ + fromClause + whereClause + " ORDER BY ws.created_at DESC LIMIT :limit OFFSET :offset",
            params.addValue("limit", size).addValue("offset", page * size),
            (rs, rowNum) -> new ShipmentJoinedRow(
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
                toInstant(rs.getTimestamp("released_at")),
                rs.getString("released_by_user_id"),
                rs.getString("released_by_tenant_id"),
                rs.getString("evidence_group_id"),
                toInstant(rs.getTimestamp("returned_at")),
                rs.getString("returned_by_user_id"),
                rs.getString("return_evidence_group_id"),
                toInstant(rs.getTimestamp("created_at"))
            )
        );

        return new PagedShipmentReadResult(content, page, size, totalElements, totalPages);
    }

    private java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
