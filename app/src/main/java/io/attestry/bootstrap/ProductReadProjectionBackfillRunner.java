package io.attestry.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductReadProjectionBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductReadProjectionBackfillRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public ProductReadProjectionBackfillRunner(
        JdbcTemplate jdbcTemplate,
        @Value("${app.product.read-projection.backfill.enabled:false}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        backfillDistributionProjection();
        backfillShipmentProjection();
        backfillShipmentEvidenceProjection();
        backfillRetailAccessProjection();

        log.info("Product read projection backfill completed.");
    }

    private void backfillDistributionProjection() {
        jdbcTemplate.update("DELETE FROM product_passport_distribution_projection");
        jdbcTemplate.update(
            """
            INSERT INTO product_passport_distribution_projection (
                passport_id,
                distribution_id,
                target_tenant_id,
                target_tenant_name,
                target_tenant_type,
                partner_link_id,
                status,
                distributed_at,
                source_event_id,
                source_event_version,
                updated_at
            )
            SELECT d.passport_id,
                   d.distribution_id,
                   d.target_tenant_id,
                   COALESCE(t.name, ''),
                   COALESCE(t.type, 'UNKNOWN'),
                   d.partner_link_id,
                   d.status,
                   d.distributed_at,
                   'backfill:distribution:' || d.distribution_id,
                   d.row_version,
                   CURRENT_TIMESTAMP
            FROM (
                SELECT DISTINCT ON (passport_id)
                       distribution_id,
                       passport_id,
                       target_tenant_id,
                       partner_link_id,
                       status,
                       distributed_at,
                       row_version
                FROM distributions
                ORDER BY passport_id, distributed_at DESC, distribution_id DESC
            ) d
            LEFT JOIN tenants t ON t.tenant_id = d.target_tenant_id
            """
        );
    }

    private void backfillShipmentProjection() {
        jdbcTemplate.update("DELETE FROM product_passport_shipment_projection");
        jdbcTemplate.update(
            """
            INSERT INTO product_passport_shipment_projection (
                passport_id,
                shipment_id,
                status,
                shipment_round,
                released_at,
                released_by_user_display,
                returned_at,
                returned_by_user_display,
                source_event_id,
                source_event_version,
                updated_at
            )
            SELECT ws.passport_id,
                   ws.shipment_id,
                   ws.status,
                   ws.shipment_round,
                   ws.released_at,
                   released_user.email,
                   ws.returned_at,
                   returned_user.email,
                   'backfill:shipment:' || ws.shipment_id,
                   NULL,
                   CURRENT_TIMESTAMP
            FROM (
                SELECT DISTINCT ON (passport_id)
                       shipment_id,
                       passport_id,
                       status,
                       shipment_round,
                       released_at,
                       released_by_user_id,
                       returned_at,
                       returned_by_user_id
                FROM workflow_shipments
                ORDER BY passport_id, shipment_round DESC, created_at DESC, shipment_id DESC
            ) ws
            LEFT JOIN user_accounts released_user ON released_user.user_id = ws.released_by_user_id
            LEFT JOIN user_accounts returned_user ON returned_user.user_id = ws.returned_by_user_id
            """
        );
    }

    private void backfillShipmentEvidenceProjection() {
        jdbcTemplate.update("DELETE FROM product_passport_shipment_evidence_projection");
        jdbcTemplate.update(
            """
            INSERT INTO product_passport_shipment_evidence_projection (
                shipment_id,
                evidence_id,
                original_file_name,
                content_type,
                size_bytes,
                object_key,
                updated_at
            )
            SELECT ws.shipment_id,
                   wse.evidence_id,
                   COALESCE(wse.original_file_name, ''),
                   COALESCE(wse.content_type, 'application/octet-stream'),
                   COALESCE(wse.size_bytes, 0),
                   COALESCE(wse.object_key, ''),
                   CURRENT_TIMESTAMP
            FROM product_passport_shipment_projection ppsp
            JOIN workflow_shipments ws ON ws.shipment_id = ppsp.shipment_id
            JOIN workflow_shipment_evidences wse ON wse.evidence_group_id IN (ws.evidence_group_id, ws.return_evidence_group_id)
            WHERE wse.status = 'READY'
            """
        );
    }

    private void backfillRetailAccessProjection() {
        jdbcTemplate.update("DELETE FROM product_retail_access_projection");

        jdbcTemplate.update(
            """
            INSERT INTO product_retail_access_projection (
                tenant_id,
                passport_id,
                access_source_type,
                access_source_id,
                source_tenant_id,
                permission_id,
                expires_at,
                access_status,
                granted_at,
                updated_at
            )
            SELECT ppm.target_tenant_id,
                   ppm.passport_id,
                   'PERMISSION',
                   ppm.permission_id,
                   ppm.source_tenant_id,
                   ppm.permission_id,
                   ppm.expires_at,
                   CASE
                       WHEN ppm.status = 'ACTIVE'
                        AND (ppm.expires_at IS NULL OR ppm.expires_at > CURRENT_TIMESTAMP)
                       THEN 'ACTIVE'
                       WHEN ppm.status = 'ACTIVE'
                       THEN 'EXPIRED'
                       ELSE ppm.status
                   END,
                   ppm.created_at,
                   CURRENT_TIMESTAMP
            FROM passport_permissions ppm
            WHERE ppm.target_tenant_id IS NOT NULL
            """
        );

        jdbcTemplate.update(
            """
            INSERT INTO product_retail_access_projection (
                tenant_id,
                passport_id,
                access_source_type,
                access_source_id,
                source_tenant_id,
                permission_id,
                expires_at,
                access_status,
                granted_at,
                updated_at
            )
            SELECT tt.tenant_id,
                   tt.passport_id,
                   'B2C_TRANSFER',
                   tt.transfer_id,
                   NULL,
                   NULL,
                   NULL,
                   'COMPLETED',
                   tt.completed_at,
                   CURRENT_TIMESTAMP
            FROM token_transfers tt
            WHERE tt.transfer_type = 'B2C'
              AND tt.status = 'COMPLETED'
              AND tt.tenant_id IS NOT NULL
              AND tt.completed_at IS NOT NULL
            """
        );
    }
}
