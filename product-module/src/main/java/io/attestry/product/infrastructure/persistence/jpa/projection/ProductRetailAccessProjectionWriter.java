package io.attestry.product.infrastructure.persistence.jpa.projection;

import io.attestry.product.application.port.projection.ProductRetailAccessProjectionWritePort;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductRetailAccessProjectionWriter implements ProductRetailAccessProjectionWritePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProductRetailAccessProjectionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void refreshB2cTransferAccess(String passportId, String transferId, String sourceEventId, Instant updatedAt) {
        Timestamp timestamp = Timestamp.from(updatedAt);

        jdbcTemplate.getJdbcOperations().update(
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
                       ?
                FROM token_transfers tt
                WHERE tt.transfer_id = ?
                  AND tt.transfer_type = 'B2C'
                  AND tt.status = 'COMPLETED'
                  AND tt.tenant_id IS NOT NULL
                  AND tt.completed_at IS NOT NULL
                ON CONFLICT (tenant_id, passport_id, access_source_type, access_source_id) DO UPDATE SET
                    access_status = EXCLUDED.access_status,
                    granted_at = EXCLUDED.granted_at,
                    updated_at = EXCLUDED.updated_at
            """,
            timestamp,
            transferId
        );
    }
}
