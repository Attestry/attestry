package io.attestry.workflow.infrastructure.persistence.jpa;

import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcShipmentRepositoryAdapter implements ShipmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcShipmentRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsActiveReleasedByPassportId(String passportId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM workflow_shipments WHERE passport_id = ? AND status = 'RELEASED'",
            Integer.class,
            passportId
        );
        return count != null && count > 0;
    }

    @Override
    public int nextShipmentRound(String passportId) {
        Integer max = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(shipment_round), 0) FROM workflow_shipments WHERE passport_id = ?",
            Integer.class,
            passportId
        );
        return (max == null ? 0 : max) + 1;
    }

    @Override
    public Shipment saveRelease(Shipment shipment) {
        jdbcTemplate.update(
            """
                INSERT INTO workflow_shipments (
                    shipment_id, tenant_id, passport_id, shipment_round, status,
                    released_at, released_by_user_id,
                    evidence_group_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            shipment.shipmentId(),
            shipment.tenantId(),
            shipment.passportId(),
            shipment.shipmentRound(),
            shipment.status().name(),
            Timestamp.from(shipment.releasedAt()),
            shipment.releasedByUserId(),
            shipment.evidenceGroupId(),
            Timestamp.from(shipment.createdAt())
        );

        return jdbcTemplate.queryForObject(
            "SELECT * FROM workflow_shipments WHERE shipment_id = ?",
            (rs, rowNum) -> mapShipment(rs),
            shipment.shipmentId()
        );
    }

    @Override
    public Shipment saveReturn(Shipment shipment) {
        int updated = jdbcTemplate.update(
            """
                UPDATE workflow_shipments
                SET status = ?,
                    returned_at = ?,
                    returned_by_user_id = ?,
                    return_evidence_group_id = ?
                WHERE shipment_id = ?
                  AND status = 'RELEASED'
            """,
            shipment.status().name(),
            Timestamp.from(shipment.returnedAt()),
            shipment.returnedByUserId(),
            shipment.returnEvidenceGroupId(),
            shipment.shipmentId()
        );
        if (updated == 0) {
            throw new IllegalStateException("Failed to update shipment return state: " + shipment.shipmentId());
        }
        return jdbcTemplate.queryForObject(
            "SELECT * FROM workflow_shipments WHERE shipment_id = ?",
            (rs, rowNum) -> mapShipment(rs),
            shipment.shipmentId()
        );
    }

    @Override
    public List<Shipment> findByPassportId(String passportId) {
        return jdbcTemplate.query(
            """
                SELECT *
                FROM workflow_shipments
                WHERE passport_id = ?
                ORDER BY shipment_round DESC, created_at DESC
            """,
            (rs, rowNum) -> mapShipment(rs),
            passportId
        );
    }

    @Override
    public Optional<Shipment> findByShipmentId(String shipmentId) {
        List<Shipment> rows = jdbcTemplate.query(
            "SELECT * FROM workflow_shipments WHERE shipment_id = ?",
            (rs, rowNum) -> mapShipment(rs),
            shipmentId
        );
        return rows.stream().findFirst();
    }

    private Shipment mapShipment(ResultSet rs) throws SQLException {
        Timestamp returnedAt = rs.getTimestamp("returned_at");
        return new Shipment(
            rs.getString("shipment_id"),
            rs.getString("tenant_id"),
            rs.getString("passport_id"),
            rs.getInt("shipment_round"),
            ShipmentStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("released_at").toInstant(),
            rs.getString("released_by_user_id"),
            rs.getString("released_by_tenant_id"),
            rs.getString("evidence_group_id"),
            returnedAt == null ? null : returnedAt.toInstant(),
            rs.getString("returned_by_user_id"),
            rs.getString("return_evidence_group_id"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
