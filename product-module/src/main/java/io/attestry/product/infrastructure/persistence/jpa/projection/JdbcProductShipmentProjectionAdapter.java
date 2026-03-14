package io.attestry.product.infrastructure.persistence.jpa.projection;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import io.attestry.product.application.port.projection.ProductShipmentProjectionPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@RequiredArgsConstructor
public class JdbcProductShipmentProjectionAdapter
    implements ProductShipmentProjectionPort, PassportShipmentQueryPort {

    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(30);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectStoragePort objectStoragePort;

    @Override
    public Optional<ShipmentProjectionView> findLatestShipment(String passportId) {
        return findLatestShipmentProjection(passportId);
    }

    @Override
    public Optional<ShipmentRecord> findLatestShipmentByPassportId(String passportId) {
        return findLatestShipmentProjection(passportId)
            .map(view -> new ShipmentRecord(
                view.shipmentId(),
                view.status(),
                view.shipmentRound(),
                view.releasedAt(),
                view.releasedByUserDisplay(),
                view.returnedAt(),
                view.returnedByUserDisplay(),
                view.evidenceFiles().stream()
                    .map(file -> new EvidenceFileRecord(
                        file.evidenceId(),
                        file.originalFileName(),
                        file.contentType(),
                        file.sizeBytes(),
                        file.objectKey() == null || file.objectKey().isBlank()
                            ? null
                            : objectStoragePort.issuePresignedDownload(file.objectKey(), DOWNLOAD_TTL).downloadUrl()
                    ))
                    .toList()
            ));
    }

    private Optional<ShipmentProjectionView> findLatestShipmentProjection(String passportId) {
        return jdbcTemplate.getJdbcOperations().query(
            """
                SELECT passport_id,
                       shipment_id,
                       status,
                       shipment_round,
                       released_at,
                       released_by_user_display,
                       returned_at,
                       returned_by_user_display,
                       updated_at
                FROM product_passport_shipment_projection
                WHERE passport_id = ?
            """,
            rs -> rs.next()
                ? Optional.of(toShipmentProjectionView(rs))
                : Optional.empty(),
            passportId
        );
    }

    private ShipmentProjectionView toShipmentProjectionView(ResultSet rs) throws SQLException {
        String shipmentId = rs.getString("shipment_id");
        List<ShipmentEvidenceProjectionView> evidenceFiles = jdbcTemplate.getJdbcOperations().query(
            """
                SELECT evidence_id,
                       original_file_name,
                       content_type,
                       size_bytes,
                       object_key
                FROM product_passport_evidence_projection
                WHERE shipment_id = ?
                ORDER BY evidence_id
            """,
            evidenceRowMapper(),
            shipmentId
        );

        return new ShipmentProjectionView(
            rs.getString("passport_id"),
            shipmentId,
            rs.getString("status"),
            rs.getInt("shipment_round"),
            toInstant(rs.getTimestamp("released_at")),
            rs.getString("released_by_user_display"),
            toInstant(rs.getTimestamp("returned_at")),
            rs.getString("returned_by_user_display"),
            evidenceFiles,
            toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private RowMapper<ShipmentEvidenceProjectionView> evidenceRowMapper() {
        return (rs, rowNum) -> toEvidenceView(rs);
    }

    private ShipmentEvidenceProjectionView toEvidenceView(ResultSet rs) throws SQLException {
        return new ShipmentEvidenceProjectionView(
            rs.getString("evidence_id"),
            rs.getString("original_file_name"),
            rs.getString("content_type"),
            rs.getLong("size_bytes"),
            rs.getString("object_key")
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
