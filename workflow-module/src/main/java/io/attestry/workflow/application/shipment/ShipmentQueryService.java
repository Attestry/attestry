package io.attestry.workflow.application.shipment;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.shipment.assembler.ShipmentEvidenceViewAssembler;
import io.attestry.workflow.application.shipment.assembler.ShipmentQueryViewAssembler;
import io.attestry.workflow.application.shipment.policy.ShipmentQueryAccessPolicy;
import io.attestry.workflow.application.shipment.result.ShipmentDetailResult;
import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import io.attestry.workflow.application.usecase.ShipmentQueryUseCase;
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
    public List<ShipmentViewResult> listByPassport(
        AuthPrincipal principal,
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
    public PagedShipmentViewResponse listByTenant(
        AuthPrincipal principal,
        int page,
        int size,
        String keyword
    ) {
        String tenantId = accessPolicy.assertTenantShipmentListAccess(principal);
        ShipmentProductReadPort.PagedShipmentReadResult result = shipmentProductReadPort
            .findShipmentsByTenantId(tenantId, page, size, keyword);
        return viewAssembler.toPagedShipmentViewResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDetailResult getShipmentDetail(
        AuthPrincipal principal,
        String shipmentId
    ) {
        Shipment shipment = shipmentRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new WorkflowDomainException(
                WorkflowErrorCode.INVALID_REQUEST,
                "Shipment not found"
            ));
        accessPolicy.assertShipmentDetailAccess(principal, shipment);

        List<ShipmentDetailResult.EvidenceFileResult> releaseFiles =
            evidenceViewAssembler.toDetailEvidenceFiles(shipment.evidenceGroupId());
        List<ShipmentDetailResult.EvidenceFileResult> returnFiles =
            evidenceViewAssembler.toDetailEvidenceFiles(shipment.returnEvidenceGroupId());

        ShipmentProductReadPort.PassportAssetInfo assetInfo = shipmentProductReadPort
            .findPassportAssetInfoByIds(List.of(shipment.passportId()))
            .get(shipment.passportId());

        List<String> userIds = Stream.of(shipment.releasedByUserId(), shipment.returnedByUserId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<String, String> emailMap = userReadPort.findEmailsByUserIds(userIds);

        return viewAssembler.toShipmentDetailResult(shipment, assetInfo, emailMap, releaseFiles, returnFiles);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedReleaseCandidateResponse listReleaseCandidates(
        AuthPrincipal principal,
        int page,
        int size,
        String keyword
    ) {
        String tenantId = accessPolicy.assertReleaseCandidateAccess(principal);
        ShipmentProductReadPort.PagedReleaseCandidateResult result = shipmentProductReadPort
            .findReleaseCandidatesByTenantId(tenantId, page, size, keyword);
        return viewAssembler.toPagedReleaseCandidateResponse(result);
    }

    private List<ShipmentViewResult> enrichShipments(List<Shipment> shipments) {
        if (shipments.isEmpty()) {
            return List.of();
        }
        List<String> passportIds = shipments.stream().map(Shipment::passportId).distinct().toList();
        Map<String, ShipmentProductReadPort.PassportAssetInfo> assetMap =
            shipmentProductReadPort.findPassportAssetInfoByIds(passportIds);
        return viewAssembler.toShipmentViewResults(shipments, assetMap);
    }
}
