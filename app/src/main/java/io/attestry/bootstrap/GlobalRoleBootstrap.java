package io.attestry.bootstrap;

import io.attestry.userauth.domain.authorization.model.RoleCodes;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class GlobalRoleBootstrap implements ApplicationRunner {

    private static final List<RoleSeed> ROLE_SEEDS = List.of(
        new RoleSeed("role-platform-super-admin", RoleCodes.PLATFORM_SUPER_ADMIN, "Platform Super Admin", "Platform super admin role"),
        new RoleSeed("role-owner-default", RoleCodes.OWNER_DEFAULT, "Owner Default", "Owner baseline role"),
        new RoleSeed("role-tenant-owner", RoleCodes.TENANT_OWNER, "Tenant Owner", "Tenant owner role"),
        new RoleSeed("role-tenant-operator", RoleCodes.TENANT_OPERATOR, "Tenant Operator", "Tenant operator role"),
        new RoleSeed("role-tenant-staff", RoleCodes.TENANT_STAFF, "Tenant Staff", "Tenant staff role")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GlobalRoleBootstrap(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleSeed roleSeed : ROLE_SEEDS) {
            jdbcTemplate.getJdbcOperations().update(
                """
                INSERT INTO roles (role_id, tenant_id, code, name, description, group_type, enabled)
                SELECT ?, NULL, ?, ?, ?, 'ANY', TRUE
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM roles
                    WHERE tenant_id IS NULL AND code = ?
                )
                """,
                roleSeed.roleId(),
                roleSeed.code(),
                roleSeed.name(),
                roleSeed.description(),
                roleSeed.code()
            );
        }
    }

    private record RoleSeed(
        String roleId,
        String code,
        String name,
        String description
    ) {
    }
}
