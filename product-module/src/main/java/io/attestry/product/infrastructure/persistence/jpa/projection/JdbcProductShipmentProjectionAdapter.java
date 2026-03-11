package io.attestry.product.infrastructure.persistence.jpa.projection;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import io.attestry.product.application.port.projection.ProductShipmentProjectionPort;
import io.attestry.product.infrastructure.persistence.jpa.entity.PassportShipmentProjectionJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.entity.ShipmentEvidenceProjectionJpaEntity;
import io.attestry.product.infrastructure.persistence.jpa.repository.PassportShipmentProjectionJpaRepository;
import io.attestry.product.infrastructure.persistence.jpa.repository.ShipmentEvidenceProjectionJpaRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
@RequiredArgsConstructor
public class JdbcProductShipmentProjectionAdapter
    implements ProductShipmentProjectionPort, PassportShipmentQueryPort {

    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(30);

    private final PassportShipmentProjectionJpaRepository shipmentRepository;
    private final ShipmentEvidenceProjectionJpaRepository evidenceRepository;
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
        return shipmentRepository.findById(passportId)
            .map(entity -> {
                List<ShipmentEvidenceProjectionView> evidenceFiles =
                    evidenceRepository.findByShipmentIdOrderByEvidenceId(entity.getShipmentId())
                        .stream()
                        .map(this::toEvidenceView)
                        .toList();

                return new ShipmentProjectionView(
                    entity.getPassportId(),
                    entity.getShipmentId(),
                    entity.getStatus(),
                    entity.getShipmentRound(),
                    entity.getReleasedAt(),
                    entity.getReleasedByUserDisplay(),
                    entity.getReturnedAt(),
                    entity.getReturnedByUserDisplay(),
                    evidenceFiles,
                    entity.getUpdatedAt()
                );
            });
    }

    private ShipmentEvidenceProjectionView toEvidenceView(ShipmentEvidenceProjectionJpaEntity entity) {
        return new ShipmentEvidenceProjectionView(
            entity.getEvidenceId(),
            entity.getOriginalFileName(),
            entity.getContentType(),
            entity.getSizeBytes(),
            entity.getObjectKey()
        );
    }
}
