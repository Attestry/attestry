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
    public void refreshShipmentProjection(ShipmentPayload payload, String sourceEventId, Long sourceEventVersion, Instant updatedAt) {
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
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            payload.passportId(),
            payload.shipmentId(),
            payload.status(),
            payload.shipmentRound(),
            payload.releasedAt() != null ? Timestamp.from(payload.releasedAt()) : null,
            payload.releasedByUserDisplay(),
            payload.returnedAt() != null ? Timestamp.from(payload.returnedAt()) : null,
            payload.returnedByUserDisplay(),
            sourceEventId,
            sourceEventVersion,
            timestamp
        );

        refreshEvidenceProjection(payload);
    }

    private void refreshEvidenceProjection(ShipmentPayload payload) {
        jdbcTemplate.getJdbcOperations().update(
            """
                DELETE FROM product_passport_evidence_projection
                WHERE shipment_id IN (
                    SELECT shipment_id FROM product_passport_shipment_projection WHERE passport_id = ?
                )
            """,
            payload.passportId()
        );

        if (payload.evidences() == null || payload.evidences().isEmpty()) {
            return;
        }

        for (EvidencePayload evidence : payload.evidences()) {
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
                    ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                payload.shipmentId(),
                evidence.evidenceId(),
                evidence.originalFileName() == null ? "" : evidence.originalFileName(),
                evidence.contentType() == null ? "application/octet-stream" : evidence.contentType(),
                evidence.sizeBytes(),
                evidence.objectKey() == null ? "" : evidence.objectKey()
            );
        }
    }
}
