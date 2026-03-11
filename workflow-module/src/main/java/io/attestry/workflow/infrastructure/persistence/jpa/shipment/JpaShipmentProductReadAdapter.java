package io.attestry.workflow.infrastructure.persistence.jpa.shipment;

import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.ShipmentJpaRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.WorkflowPassportCatalogProjectionJpaRepository;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.WorkflowPassportCatalogProjectionJpaRepository.PassportAssetInfoProjection;
import io.attestry.workflow.infrastructure.persistence.jpa.repository.WorkflowPassportStateProjectionJpaRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaShipmentProductReadAdapter implements ShipmentProductReadPort {

    private final WorkflowPassportStateProjectionJpaRepository stateRepository;
    private final WorkflowPassportCatalogProjectionJpaRepository catalogRepository;
    private final ShipmentJpaRepository shipmentRepository;

    @Override
    public Optional<PassportState> findPassportState(String passportId) {
        return stateRepository.findPassportStateById(passportId)
            .map(p -> new PassportState(p.getPassportId(), p.getTenantId(), p.getAssetState(), p.getRiskFlag()));
    }

    @Override
    public Map<String, PassportAssetInfo> findPassportAssetInfoByIds(List<String> passportIds) {
        if (passportIds == null || passportIds.isEmpty()) return Collections.emptyMap();
        return catalogRepository.findAssetInfoByPassportIds(passportIds).stream()
            .map(this::toAssetInfo)
            .collect(Collectors.toMap(PassportAssetInfo::passportId, Function.identity()));
    }

    @Override
    public PagedReleaseCandidateResult findReleaseCandidatesByTenantId(
        String tenantId, int page, int size, String keyword
    ) {
        return shipmentRepository.findReleaseCandidatesWithFilters(tenantId, page, size, keyword);
    }

    @Override
    public PagedShipmentReadResult findShipmentsByTenantId(
        String tenantId, int page, int size, String keyword
    ) {
        return shipmentRepository.findShipmentsWithFilters(tenantId, page, size, keyword);
    }

    private PassportAssetInfo toAssetInfo(PassportAssetInfoProjection p) {
        return new PassportAssetInfo(
            p.getPassportId(), p.getAssetId(), p.getSerialNumber(),
            p.getModelId(), p.getModelName(), p.getProductionBatch(), p.getFactoryCode()
        );
    }
}
