package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.ShipmentProductReadPort;
import java.util.List;
import java.util.Optional;
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
    public List<ShipmentReleaseCandidate> findReleaseCandidatesByTenantId(String tenantId) {
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
                 WHERE pp.tenant_id = ?
                   AND pa.asset_state = 'ACTIVE'
                   AND pa.risk_flag = 'NONE'
                   AND NOT EXISTS (
                        SELECT 1
                          FROM workflow_shipments ws
                         WHERE ws.passport_id = pp.passport_id
                           AND ws.status = 'RELEASED'
                   )
                 ORDER BY pp.created_at DESC
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
            tenantId
        );
    }
}
