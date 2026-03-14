package io.attestry.product.infrastructure.persistence.jpa.projection;

import io.attestry.product.application.port.projection.ProductShipmentProjectionWritePort;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductShipmentProjectionWriter implements ProductShipmentProjectionWritePort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProductShipmentProjectionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void refreshShipmentProjection(String passportId, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
        Timestamp timestamp = Timestamp.from(updatedAt);

        jdbcTemplate.getJdbcOperations().update(
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
                       ?,
                       ?,
                       ?
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
                    FROM shipments
                    WHERE passport_id = ?
                    ORDER BY passport_id, shipment_round DESC, created_at DESC, shipment_id DESC
                ) ws
                LEFT JOIN user_accounts released_user ON released_user.user_id = ws.released_by_user_id
                LEFT JOIN user_accounts returned_user ON returned_user.user_id = ws.returned_by_user_id
                ON CONFLICT (passport_id) DO UPDATE SET
                    shipment_id = EXCLUDED.shipment_id,
                    status = EXCLUDED.status,
                    shipment_round = EXCLUDED.shipment_round,
                    released_at = EXCLUDED.released_at,
                    released_by_user_display = EXCLUDED.released_by_user_display,
                    returned_at = EXCLUDED.returned_at,
                    returned_by_user_display = EXCLUDED.returned_by_user_display,
                    source_event_id = EXCLUDED.source_event_id,
                    source_event_version = EXCLUDED.source_event_version,
                    updated_at = EXCLUDED.updated_at
            """,
            sourceEventId,
            sourceEventVersion,
            timestamp,
            passportId
        );

        refreshEvidenceProjection(passportId);
    }

    private void refreshEvidenceProjection(String passportId) {
        jdbcTemplate.getJdbcOperations().update(
            """
                DELETE FROM product_passport_evidence_projection
                WHERE shipment_id IN (
                    SELECT shipment_id FROM product_passport_shipment_projection WHERE passport_id = ?
                )
            """,
            passportId
        );

        jdbcTemplate.getJdbcOperations().update(
            """
                INSERT INTO product_passport_evidence_projection (
                    shipment_id,
                    evidence_id,
                    original_file_name,
                    content_type,
                    size_bytes,
                    object_key,
                    updated_at
                )
                SELECT ws.shipment_id,
                       we.evidence_id,
                       COALESCE(we.original_file_name, ''),
                       COALESCE(we.content_type, 'application/octet-stream'),
                       COALESCE(we.size_bytes, 0),
                       COALESCE(we.object_key, ''),
                       CURRENT_TIMESTAMP
                FROM product_passport_shipment_projection ppsp
                JOIN shipments ws ON ws.shipment_id = ppsp.shipment_id
                JOIN workflow_evidences we ON we.evidence_group_id IN (ws.evidence_group_id, ws.return_evidence_group_id)
                WHERE ppsp.passport_id = ?
                  AND we.status = 'READY'
            """,
            passportId
        );
    }
}
