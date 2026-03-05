package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.ServiceProductReadPort;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcServiceProductReadAdapter implements ServiceProductReadPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcServiceProductReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ServicePassportState> findPassportState(String passportId) {
        List<ServicePassportState> rows = jdbcTemplate.query(
            """
                SELECT pp.passport_id, pp.tenant_id, pp.group_id, pa.asset_state, pa.risk_flag
                FROM product_passports pp
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                WHERE pp.passport_id = ?
            """,
            (rs, rowNum) -> new ServicePassportState(
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

    @Override
    public Optional<String> findCurrentOwnerId(String passportId) {
        List<String> rows = jdbcTemplate.query(
            """
                SELECT owner_user_id FROM passport_ownership
                WHERE passport_id = ?
                ORDER BY created_at DESC
                LIMIT 1
            """,
            (rs, rowNum) -> rs.getString("owner_user_id"),
            passportId
        );
        return rows.stream().findFirst();
    }
}
