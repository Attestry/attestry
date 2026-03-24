package io.attestry.workflow.application.shipment.query;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.shipment.assembler.ShipmentEvidenceViewAssembler;
import io.attestry.workflow.application.shipment.assembler.ShipmentQueryViewAssembler;
import io.attestry.workflow.application.shipment.policy.ShipmentQueryAccessPolicy;
import io.attestry.workflow.application.shipment.usecase.ShipmentQueryUseCase;
import io.attestry.workflow.application.shipment.view.PagedReleaseCandidateView;
import io.attestry.workflow.application.shipment.view.PagedShipmentView;
import io.attestry.workflow.application.shipment.view.ShipmentDetailView;
import io.attestry.workflow.application.shipment.view.ShipmentView;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ShipmentQueryService implements ShipmentQueryUseCase {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentProductReadPort shipmentProductReadPort;
    private final UserReadPort userReadPort;
    private final ShipmentQueryAccessPolicy accessPolicy;
    private final ShipmentEvidenceViewAssembler evidenceViewAssembler;
    private final ShipmentQueryViewAssembler viewAssembler;

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentView> listByPassport(
        WorkflowActorContext principal,
        String passportId
    ) {
        String tenantId = accessPolicy.assertPassportListAccess(principal, passportId);
        List<Shipment> shipments = shipmentRepository.findByPassportId(passportId).stream()
            .filter(shipment -> tenantId.equals(shipment.tenantId()))
            .toList();
        return enrichShipments(shipments);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedShipmentView listByTenant(
        WorkflowActorContext principal,
        int page,
        int size,
        String keyword
    ) {
        String tenantId = accessPolicy.assertTenantShipmentListAccess(principal);
        ShipmentProductReadPort.PagedShipmentReadResult result = shipmentProductReadPort
            .findShipmentsByTenantId(tenantId, page, size, keyword);
        return viewAssembler.toPagedShipmentView(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDetailView getShipmentDetail(
        WorkflowActorContext principal,
        String shipmentId
    ) {
        Shipment shipment = shipmentRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Shipment not found"
            ));
        accessPolicy.assertShipmentDetailAccess(principal, shipment);

        List<ShipmentDetailView.EvidenceFileView> releaseFiles =
            evidenceViewAssembler.toDetailEvidenceFiles(shipment.evidenceGroupId());
        List<ShipmentDetailView.EvidenceFileView> returnFiles =
            evidenceViewAssembler.toDetailEvidenceFiles(shipment.returnEvidenceGroupId());

        ShipmentProductReadPort.PassportAssetInfo assetInfo = loadAssetInfo(shipment.passportId());
        Map<String, String> emailMap = loadUserEmails(shipment);

        return viewAssembler.toShipmentDetailView(shipment, assetInfo, emailMap, releaseFiles, returnFiles);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedReleaseCandidateView listReleaseCandidates(
        WorkflowActorContext principal,
        int page,
        int size,
        String keyword
    ) {
        String tenantId = accessPolicy.assertReleaseCandidateAccess(principal);
        ShipmentProductReadPort.PagedReleaseCandidateResult result = shipmentProductReadPort
            .findReleaseCandidatesByTenantId(tenantId, page, size, keyword);
        return viewAssembler.toPagedReleaseCandidateView(result);
    }

    private List<ShipmentView> enrichShipments(List<Shipment> shipments) {
        if (shipments.isEmpty()) {
            return List.of();
        }
        List<String> passportIds = shipments.stream().map(Shipment::passportId).distinct().toList();
        Map<String, ShipmentProductReadPort.PassportAssetInfo> assetMap =
            shipmentProductReadPort.findPassportAssetInfoByIds(passportIds);
        return viewAssembler.toShipmentViews(shipments, assetMap);
    }

    private ShipmentProductReadPort.PassportAssetInfo loadAssetInfo(String passportId) {
        return shipmentProductReadPort.findPassportAssetInfoByIds(List.of(passportId)).get(passportId);
    }

    private Map<String, String> loadUserEmails(Shipment shipment) {
        List<String> userIds = Stream.of(shipment.releasedByUserId(), shipment.returnedByUserId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        return userReadPort.findEmailMapByUserIds(userIds);
    }
}
