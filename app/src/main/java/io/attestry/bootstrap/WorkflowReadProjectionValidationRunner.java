package io.attestry.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkflowReadProjectionValidationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowReadProjectionValidationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public WorkflowReadProjectionValidationRunner(
        JdbcTemplate jdbcTemplate,
        @Value("${app.workflow.read-projection.validation.enabled:false}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        validatePassportStateProjection();
        validatePassportCatalogProjection();
        validatePassportPermissionProjection();
        validatePassportOwnershipProjection();
    }

    private void validatePassportStateProjection() {
        long sourceCount = count("""
            SELECT COUNT(*)
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
        """);
        long projectionCount = count("SELECT COUNT(*) FROM workflow_passport_state_projection");
        long missingCount = count("""
            SELECT COUNT(*)
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
            LEFT JOIN workflow_passport_state_projection wpsp
              ON wpsp.passport_id = pp.passport_id
            WHERE wpsp.passport_id IS NULL
        """);

        log.info(
            "Workflow passport state projection validation: sourceCount={}, projectionCount={}, missingCount={}",
            sourceCount,
            projectionCount,
            missingCount
        );
    }

    private void validatePassportCatalogProjection() {
        long sourceCount = count("""
            SELECT COUNT(*)
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
        """);
        long projectionCount = count("SELECT COUNT(*) FROM workflow_passport_catalog_projection");
        long missingCount = count("""
            SELECT COUNT(*)
            FROM product_passports pp
            JOIN product_assets pa ON pa.asset_id = pp.asset_id
            LEFT JOIN workflow_passport_catalog_projection wpcp
              ON wpcp.passport_id = pp.passport_id
            WHERE wpcp.passport_id IS NULL
        """);

        log.info(
            "Workflow passport catalog projection validation: sourceCount={}, projectionCount={}, missingCount={}",
            sourceCount,
            projectionCount,
            missingCount
        );
    }

    private void validatePassportPermissionProjection() {
        long sourceCount = count("""
            SELECT COUNT(*)
            FROM passport_permissions pp
            WHERE pp.seller_tenant_id IS NOT NULL
        """);
        long projectionCount = count("SELECT COUNT(*) FROM workflow_passport_permission_projection");
        long missingCount = count("""
            SELECT COUNT(*)
            FROM passport_permissions pp
            LEFT JOIN workflow_passport_permission_projection wppp
              ON wppp.permission_id = pp.permission_id
            WHERE pp.seller_tenant_id IS NOT NULL
              AND wppp.permission_id IS NULL
        """);

        log.info(
            "Workflow passport permission projection validation: sourceCount={}, projectionCount={}, missingCount={}",
            sourceCount,
            projectionCount,
            missingCount
        );
    }

    private void validatePassportOwnershipProjection() {
        long sourceCount = count("""
            SELECT COUNT(*)
            FROM passport_ownership po
            WHERE po.owner_id IS NOT NULL
        """);
        long projectionCount = count("SELECT COUNT(*) FROM workflow_passport_ownership_projection");
        long missingCount = count("""
            SELECT COUNT(*)
            FROM passport_ownership po
            LEFT JOIN workflow_passport_ownership_projection wpop
              ON wpop.passport_id = po.passport_id
            WHERE po.owner_id IS NOT NULL
              AND wpop.passport_id IS NULL
        """);

        log.info(
            "Workflow passport ownership projection validation: sourceCount={}, projectionCount={}, missingCount={}",
            sourceCount,
            projectionCount,
            missingCount
        );
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
}
