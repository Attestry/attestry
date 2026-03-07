package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.TenantReadPort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTenantReadAdapter implements TenantReadPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsActiveTenant(String tenantId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tenants WHERE tenant_id = ? AND status = 'ACTIVE'",
            Integer.class,
            tenantId
        );
        return count != null && count > 0;
    }

    @Override
    public String findTenantName(String tenantId) {
        List<String> names = jdbcTemplate.queryForList(
            "SELECT name FROM tenants WHERE tenant_id = ?",
            String.class,
            tenantId
        );
        return names.isEmpty() ? null : names.get(0);
    }

    @Override
    public String findTenantType(String tenantId) {
        List<String> types = jdbcTemplate.queryForList(
            "SELECT type FROM tenants WHERE tenant_id = ?",
            String.class,
            tenantId
        );
        return types.isEmpty() ? null : types.get(0);
    }

    @Override
    public TenantSummary findTenantSummary(String tenantId) {
        List<TenantSummary> results = jdbcTemplate.query(
            "SELECT tenant_id, name, region, type FROM tenants WHERE tenant_id = ?",
            (rs, rowNum) -> new TenantSummary(
                rs.getString("tenant_id"),
                rs.getString("name"),
                rs.getString("region"),
                rs.getString("type")
            ),
            tenantId
        );
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<TenantSummary> searchActiveTenantsByName(String name) {
        String keyword = "%" + name + "%";
        return jdbcTemplate.query(
            """
                SELECT tenant_id, name, region, type
                  FROM tenants
                 WHERE status = 'ACTIVE'
                   AND LOWER(name) LIKE LOWER(?)
                 ORDER BY name ASC
            """,
            (rs, rowNum) -> new TenantSummary(
                rs.getString("tenant_id"),
                rs.getString("name"),
                rs.getString("region"),
                rs.getString("type")
            ),
            keyword
        );
    }
}
