package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.application.port.MyPassportQueryPort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMyPassportQueryAdapter implements MyPassportQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMyPassportQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MyPassportView> findByOwnerId(String ownerId) {
        return jdbcTemplate.query(
            """
                SELECT pp.passport_id, pp.qr_public_code, pp.tenant_id,
                       pa.asset_id, pa.serial_number, pa.model_name,
                       pa.asset_state, pa.risk_flag,
                       po.updated_at
                FROM passport_ownership po
                JOIN product_passports pp ON pp.passport_id = po.passport_id
                JOIN product_assets pa ON pa.asset_id = pp.asset_id
                WHERE po.owner_id = ?
                ORDER BY po.updated_at DESC
                """,
            (rs, rowNum) -> new MyPassportView(
                rs.getString("passport_id"),
                rs.getString("qr_public_code"),
                rs.getString("tenant_id"),
                rs.getString("asset_id"),
                rs.getString("serial_number"),
                rs.getString("model_name"),
                rs.getString("asset_state"),
                rs.getString("risk_flag"),
                rs.getTimestamp("updated_at").toInstant()
            ),
            ownerId
        );
    }
}
