package io.attestry.workflow.application.shipment;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.shipment.result.ShipmentEvidenceViewResult;
import io.attestry.workflow.application.shipment.result.ShipmentViewResult;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ShipmentQueryUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentQueryService implements ShipmentQueryUseCase {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEvidencePort shipmentEvidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;

    public ShipmentQueryService(
        ShipmentRepository shipmentRepository,
        ShipmentEvidencePort shipmentEvidencePort,
        WorkflowAuthorizationSupport authorizationSupport
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.authorizationSupport = authorizationSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentViewResult> listByPassport(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        String passportId
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:list:" + passportId);
        return shipmentRepository.findByPassportId(passportId).stream()
            .filter(shipment -> tenantId.equals(shipment.tenantId()) && groupId.equals(shipment.groupId()))
            .map(this::toShipmentView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentEvidenceViewResult> listEvidenceByShipmentId(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        String shipmentId
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        Shipment shipment = shipmentRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Shipment not found"));
        if (!tenantId.equals(shipment.tenantId()) || !groupId.equals(shipment.groupId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant/group shipment access denied");
        }
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:evidence:" + shipment.shipmentId());
        List<ShipmentEvidencePort.ShipmentEvidenceView> evidences = new ArrayList<>(
            shipmentEvidencePort.findEvidenceByEvidenceGroupId(shipment.evidenceGroupId())
        );
        if (shipment.returnEvidenceGroupId() != null) {
            evidences.addAll(shipmentEvidencePort.findEvidenceByEvidenceGroupId(shipment.returnEvidenceGroupId()));
        }
        return evidences.stream()
            .map(evidence -> new ShipmentEvidenceViewResult(evidence.evidenceId(), evidence.evidenceGroupId(), evidence.fileHash()))
            .toList();
    }

    private ShipmentViewResult toShipmentView(Shipment shipment) {
        return new ShipmentViewResult(
            shipment.shipmentId(),
            shipment.tenantId(),
            shipment.groupId(),
            shipment.passportId(),
            shipment.shipmentRound(),
            shipment.status().name(),
            shipment.releasedAt(),
            shipment.releasedByUserId(),
            shipment.releasedByGroupId(),
            shipment.evidenceGroupId(),
            shipment.returnedAt(),
            shipment.returnedByUserId(),
            shipment.returnEvidenceGroupId(),
            shipment.createdAt()
        );
    }
}
