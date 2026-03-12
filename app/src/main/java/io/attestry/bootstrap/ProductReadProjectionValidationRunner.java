package io.attestry.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductReadProjectionValidationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductReadProjectionValidationRunner.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public ProductReadProjectionValidationRunner(
        NamedParameterJdbcTemplate jdbcTemplate,
        @Value("${app.product.read-projection.validation.enabled:false}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        validateDistributionProjection();
        validateShipmentProjection();
        validateRetailAccessProjection();
    }

    private void validateDistributionProjection() {
        long sourceCount = count("""
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT ON (passport_id) passport_id
                FROM distributions
                ORDER BY passport_id, distributed_at DESC, distribution_id DESC
            ) d
        """);
        long projectionCount = count("SELECT COUNT(*) FROM product_passport_distribution_projection");
        long missingCount = count("""
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT ON (d.passport_id)
                       d.passport_id
                FROM distributions d
                ORDER BY d.passport_id, d.distributed_at DESC, d.distribution_id DESC
            ) latest
            LEFT JOIN product_passport_distribution_projection p
              ON p.passport_id = latest.passport_id
            WHERE p.passport_id IS NULL
        """);

        log.info(
            "Product distribution projection validation: sourceCount={}, projectionCount={}, missingCount={}",
            sourceCount,
            projectionCount,
            missingCount
        );
    }

    private void validateShipmentProjection() {
        long sourceCount = count("""
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT ON (passport_id) passport_id
                FROM workflow_shipments
                ORDER BY passport_id, shipment_round DESC, created_at DESC, shipment_id DESC
            ) s
        """);
        long projectionCount = count("SELECT COUNT(*) FROM product_passport_shipment_projection");
        long missingCount = count("""
            SELECT COUNT(*)
            FROM (
                SELECT DISTINCT ON (ws.passport_id)
                       ws.passport_id
                FROM workflow_shipments ws
                ORDER BY ws.passport_id, ws.shipment_round DESC, ws.created_at DESC, ws.shipment_id DESC
            ) latest
            LEFT JOIN product_passport_shipment_projection p
              ON p.passport_id = latest.passport_id
            WHERE p.passport_id IS NULL
        """);

        log.info(
            "Product shipment projection validation: sourceCount={}, projectionCount={}, missingCount={}",
            sourceCount,
            projectionCount,
            missingCount
        );
    }

    private void validateRetailAccessProjection() {
        long permissionSourceCount = count("""
            SELECT COUNT(*)
            FROM passport_permissions ppm
            WHERE ppm.target_tenant_id IS NOT NULL
        """);
        long transferSourceCount = count("""
            SELECT COUNT(*)
            FROM token_transfers tt
            WHERE tt.transfer_type = 'B2C'
              AND tt.status = 'COMPLETED'
              AND tt.tenant_id IS NOT NULL
              AND tt.completed_at IS NOT NULL
        """);
        long projectionCount = count("SELECT COUNT(*) FROM product_retail_access_projection");
        long missingPermissionCount = count("""
            SELECT COUNT(*)
            FROM passport_permissions ppm
            LEFT JOIN product_retail_access_projection p
              ON p.tenant_id = ppm.target_tenant_id
             AND p.passport_id = ppm.passport_id
             AND p.access_source_type = 'PERMISSION'
             AND p.access_source_id = ppm.permission_id
            WHERE ppm.target_tenant_id IS NOT NULL
              AND p.access_source_id IS NULL
        """);
        long missingTransferCount = count("""
            SELECT COUNT(*)
            FROM token_transfers tt
            LEFT JOIN product_retail_access_projection p
              ON p.tenant_id = tt.tenant_id
             AND p.passport_id = tt.passport_id
             AND p.access_source_type = 'B2C_TRANSFER'
             AND p.access_source_id = tt.transfer_id
            WHERE tt.transfer_type = 'B2C'
              AND tt.status = 'COMPLETED'
              AND tt.tenant_id IS NOT NULL
              AND tt.completed_at IS NOT NULL
              AND p.access_source_id IS NULL
        """);

        log.info(
            "Product retail access projection validation: permissionSourceCount={}, transferSourceCount={}, projectionCount={}, missingPermissionCount={}, missingTransferCount={}",
            permissionSourceCount,
            transferSourceCount,
            projectionCount,
            missingPermissionCount,
            missingTransferCount
        );
    }

    private long count(String sql) {
        Long result = jdbcTemplate.getJdbcOperations().queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
}
