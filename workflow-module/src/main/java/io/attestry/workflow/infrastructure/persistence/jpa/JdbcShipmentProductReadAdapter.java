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
                SELECT pp.passport_id, pp.tenant_id, pp.group_id, pa.asset_state, pa.risk_flag
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                WHERE pp.passport_id = ?
            """,
            (rs, rowNum) -> new PassportState(
                rs.getString("passport_id"),
                rs.getString("tenant_id"),
                rs.getString("group_id"),
                rs.getString("asset_state"),
                rs.getString("risk_flag")
            ),
            passportId
        );
        return rows.stream().findFirst();
    }
}
