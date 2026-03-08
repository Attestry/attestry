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
    public Optional<DistributionView> findActiveDistribution(String passportId) {
        List<DistributionView> results = jdbcTemplate.query(
            """
                SELECT perm.seller_tenant_id,
                       t.name AS tenant_name,
                       t.type AS tenant_type,
                       perm.permission_code,
                       perm.scope,
                       perm.created_at AS granted_at
                FROM passport_permissions perm
                JOIN tenants t ON t.tenant_id = perm.seller_tenant_id
                WHERE perm.passport_id = ?
                  AND perm.scope = 'RETAIL_SALE'
                  AND perm.status = 'ACTIVE'
                ORDER BY perm.created_at DESC
                LIMIT 1
                """,
            (rs, rowNum) -> new DistributionView(
                rs.getString("seller_tenant_id"),
                rs.getString("tenant_name"),
                rs.getString("tenant_type"),
                rs.getString("permission_code"),
                rs.getString("scope"),
                rs.getTimestamp("granted_at").toInstant()
            ),
            passportId
        );
        return results.stream().findFirst();
    }
}
