package io.attestry.product.infrastructure.persistence.jpa;

import io.attestry.product.application.port.PassportDistributionQueryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPassportDistributionQueryAdapter implements PassportDistributionQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPassportDistributionQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<DistributionView> findLatestDistribution(String passportId) {
        List<DistributionView> results = jdbcTemplate.query(
            """
                SELECT d.distribution_id,
                       d.target_tenant_id,
                       t.name AS target_tenant_name,
                       t.type AS target_tenant_type,
                       d.partner_link_id,
                       d.status,
                       d.distributed_at
                FROM distributions d
                JOIN tenants t ON t.tenant_id = d.target_tenant_id
                WHERE d.passport_id = ?
                ORDER BY d.distributed_at DESC
                LIMIT 1
                """,
            (rs, rowNum) -> new DistributionView(
                rs.getString("distribution_id"),
                rs.getString("target_tenant_id"),
                rs.getString("target_tenant_name"),
                rs.getString("target_tenant_type"),
                rs.getString("partner_link_id"),
                rs.getString("status"),
                rs.getTimestamp("distributed_at").toInstant()
            ),
            passportId
        );
        return results.stream().findFirst();
    }
}
