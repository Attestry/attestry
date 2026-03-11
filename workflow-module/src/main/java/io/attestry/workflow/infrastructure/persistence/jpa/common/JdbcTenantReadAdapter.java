package io.attestry.workflow.infrastructure.persistence.jpa.common;

import io.attestry.workflow.application.port.common.TenantReadPort;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

//TODO("db에서 조회하는것이 맞는가? 그리고 왜 String 으로 반환하는가?")
@Repository
public class JdbcTenantReadAdapter implements TenantReadPort {

    private static final String ACTIVE_TENANT_STATUS = "ACTIVE";

    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsActiveTenant(String tenantId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tenants WHERE tenant_id = ? AND status = ?",
            Integer.class,
            tenantId,
            ACTIVE_TENANT_STATUS
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
            "SELECT tenant_id, name, region, address, type FROM tenants WHERE tenant_id = ?",
            (rs, rowNum) -> new TenantSummary(
                rs.getString("tenant_id"),
                rs.getString("name"),
                rs.getString("region"),
                rs.getString("address"),
                rs.getString("type")
            ),
            tenantId
        );
        return results.isEmpty() ? null : results.get(0);
    }
}
