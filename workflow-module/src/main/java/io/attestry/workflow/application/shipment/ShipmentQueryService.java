package io.attestry.workflow.application.shipment;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.result.EvidenceViewResult;
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
    private final WorkflowEvidencePort evidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;

    public ShipmentQueryService(
        ShipmentRepository shipmentRepository,
        WorkflowEvidencePort evidencePort,
        WorkflowAuthorizationSupport authorizationSupport
    ) {
        this.shipmentRepository = shipmentRepository;
        this.evidencePort = evidencePort;
        this.authorizationSupport = authorizationSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentViewResult> listByPassport(
        AuthPrincipal principal,
        String tenantId,
        String passportId
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:list:" + passportId);
        return shipmentRepository.findByPassportId(passportId).stream()
            .filter(shipment -> tenantId.equals(shipment.tenantId()))
            .map(this::toShipmentView)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceViewResult> listEvidenceByShipmentId(
        AuthPrincipal principal,
        String tenantId,
        String shipmentId
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        Shipment shipment = shipmentRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Shipment not found"));
        if (!tenantId.equals(shipment.tenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant shipment access denied");
        }
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:evidence:" + shipment.shipmentId());
        List<WorkflowEvidencePort.EvidenceView> evidences = new ArrayList<>(
            evidencePort.findEvidenceByEvidenceGroupId(shipment.evidenceGroupId())
        );
        if (shipment.returnEvidenceGroupId() != null) {
            evidences.addAll(evidencePort.findEvidenceByEvidenceGroupId(shipment.returnEvidenceGroupId()));
        }
        return evidences.stream()
            .map(evidence -> new EvidenceViewResult(evidence.evidenceId(), evidence.evidenceGroupId(), evidence.fileHash()))
            .toList();
    }

    private ShipmentViewResult toShipmentView(Shipment shipment) {
        return new ShipmentViewResult(
            shipment.shipmentId(),
            shipment.tenantId(),
            shipment.passportId(),
            shipment.shipmentRound(),
            shipment.status().name(),
            shipment.releasedAt(),
            shipment.releasedByUserId(),
            shipment.releasedByTenantId(),
            shipment.evidenceGroupId(),
            shipment.returnedAt(),
            shipment.returnedByUserId(),
            shipment.returnEvidenceGroupId(),
            shipment.createdAt()
        );
    }
}
