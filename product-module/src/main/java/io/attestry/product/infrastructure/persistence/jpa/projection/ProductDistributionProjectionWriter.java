package io.attestry.product.infrastructure.persistence.jpa.projection;

import io.attestry.product.application.port.projection.ProductDistributionProjectionWritePort;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductDistributionProjectionWriter implements ProductDistributionProjectionWritePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProductDistributionProjectionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void refreshDistributionProjection(String passportId, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        Timestamp timestamp = Timestamp.from(updatedAt);

        jdbcTemplate.getJdbcOperations().update(
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
                       ?,
                       ?,
                       ?
                FROM (
                    SELECT DISTINCT ON (passport_id)
                           distribution_id,
                           passport_id,
                           target_tenant_id,
                           partner_link_id,
                           status,
                           distributed_at
                    FROM distributions
                    WHERE passport_id = ?
                    ORDER BY passport_id, distributed_at DESC, distribution_id DESC
                ) d
                LEFT JOIN tenants t ON t.tenant_id = d.target_tenant_id
                ON CONFLICT (passport_id) DO UPDATE SET
                    distribution_id = EXCLUDED.distribution_id,
                    target_tenant_id = EXCLUDED.target_tenant_id,
                    target_tenant_name = EXCLUDED.target_tenant_name,
                    target_tenant_type = EXCLUDED.target_tenant_type,
                    partner_link_id = EXCLUDED.partner_link_id,
                    status = EXCLUDED.status,
                    distributed_at = EXCLUDED.distributed_at,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            sourceEventId,
            sourceEventVersion,
            timestamp,
            passportId
        );
    }
}
