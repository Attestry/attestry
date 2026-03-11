package io.attestry.workflow.application.shipment;

import io.attestry.product.application.port.query.PassportShipmentQueryPort;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.shipment.assembler.ShipmentEvidenceViewAssembler;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ShipmentBridgeQueryAdapter implements PassportShipmentQueryPort {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEvidenceViewAssembler evidenceViewAssembler;
    private final UserReadPort userReadPort;


    @Override
    @Transactional(readOnly = true)
    public Optional<ShipmentRecord> findLatestShipmentByPassportId(String passportId) {
        return shipmentRepository.findLatestByPassportId(passportId)
            .map(shipment -> {
                List<EvidenceFileRecord> releaseFiles = evidenceViewAssembler.toPassportEvidenceRecords(
                    shipment.evidenceGroupId()
                );
                List<EvidenceFileRecord> returnFiles = evidenceViewAssembler.toPassportEvidenceRecords(
                    shipment.returnEvidenceGroupId()
                );
                List<EvidenceFileRecord> allFiles = new ArrayList<>(releaseFiles);
                allFiles.addAll(returnFiles);
                List<String> userIds = Stream.of(shipment.releasedByUserId(), shipment.returnedByUserId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
                Map<String, String> emailMap = userReadPort.findEmailsByUserIds(userIds);
                return new ShipmentRecord(
                    shipment.shipmentId(),
                    shipment.status().name(),
                    shipment.shipmentRound(),
                    shipment.releasedAt(),
                    emailMap.getOrDefault(shipment.releasedByUserId(), null),
                    shipment.returnedAt(),
                    emailMap.getOrDefault(shipment.returnedByUserId(), null),
                    allFiles
                );
            });
    }
}
