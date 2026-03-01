package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.application.port.TenantReadPort;
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
}
