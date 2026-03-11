package io.attestry.workflow.application.shipment;

import static io.attestry.workflow.domain.WorkflowValidation.requireText;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.shipment.result.ReturnShipmentResult;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.application.usecase.ShipmentReleaseUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.model.ShipmentStatus;
import io.attestry.workflow.domain.shipment.policy.ShipmentReleasePolicy;
import io.attestry.workflow.domain.shipment.policy.ShipmentReleasePolicy.ShipmentReleaseContext;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ShipmentReleaseService implements ShipmentReleaseUseCase {

    private final ShipmentRepository shipmentRepository;
    private final WorkflowEvidencePort evidencePort;
    private final ShipmentProductReadPort shipmentProductReadPort;
    private final WorkflowLedgerOutboxPort shipmentLedgerOutboxPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final EvidenceUploadSupport evidenceUploadSupport;
    private final ShipmentReleasePolicy releasePolicy;
    private final Clock clock;


    @Override
    @Transactional
    public ReleaseShipmentResult release(
        AuthPrincipal principal,
        String passportId,
        ReleaseShipmentCommand command
    ) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:release:" + passportId);

        ShipmentProductReadPort.PassportState state = shipmentProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));
        ShipmentReleaseContext context = new ShipmentReleaseContext(
            tenantId,
            state.tenantId(),
            state.assetState(), state.riskFlag(),
            shipmentRepository.existsActiveReleasedByPassportId(passportId)
        );
        releasePolicy.assertReleasable(context);

        requireText(command.evidenceGroupId(), "evidenceGroupId");
        String evidenceGroupId = command.evidenceGroupId().trim();
        evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, evidenceGroupId, tenantId);
        List<String> evidenceHashes = evidencePort.findReadyEvidenceHashes(evidenceGroupId);
        if (evidenceHashes.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "At least one READY evidence is required");
        }

        Instant now = Instant.now(clock);
        Shipment shipment = Shipment.release(
            UUID.randomUUID().toString(),
            tenantId,
            passportId,
            shipmentRepository.nextShipmentRound(passportId),
            now,
            principal.userId(),
            principal.tenantId(),
            evidenceGroupId,
            now
        );
        Shipment saved = shipmentRepository.saveRelease(shipment);
        String outboxEventId = shipmentLedgerOutboxPort.enqueue(
            WorkflowLedgerEventEnvelope.shipmentReleased(saved, evidenceHashes)
        );

        return new ReleaseShipmentResult(
            saved.shipmentId(),
            saved.tenantId(),
            saved.passportId(),
            saved.shipmentRound(),
            saved.status().name(),
            saved.releasedAt(),
            saved.evidenceGroupId(),
            outboxEventId
        );
    }

    @Override
    @Transactional
    public ReturnShipmentResult returnShipment(
        AuthPrincipal principal,
        String shipmentId,
        ReturnShipmentCommand command
    ) {
        String tenantId = principal.tenantId();
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:return:" + shipmentId);
        Shipment current = shipmentRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Shipment not found"));
        if (!tenantId.equals(current.tenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant shipment access denied");
        }
        if (current.status() != ShipmentStatus.RELEASED) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Only RELEASED shipment can be returned");
        }

        String returnEvidenceGroupId = null;
        List<String> returnEvidenceHashes = List.of();
        if (command.returnEvidenceGroupId() != null && !command.returnEvidenceGroupId().isBlank()) {
            returnEvidenceGroupId = command.returnEvidenceGroupId().trim();
            evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, returnEvidenceGroupId, tenantId);
            returnEvidenceHashes = evidencePort.findReadyEvidenceHashes(returnEvidenceGroupId);
            if (returnEvidenceHashes.isEmpty()) {
                throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "returnEvidenceGroupId has no READY evidences");
            }
        }

        Instant now = Instant.now(clock);
        Shipment returned = current.markReturned(principal.userId(), returnEvidenceGroupId, now);
        Shipment saved = shipmentRepository.saveReturn(returned);
        String outboxEventId = shipmentLedgerOutboxPort.enqueue(
            WorkflowLedgerEventEnvelope.shipmentReturned(saved, returnEvidenceHashes, command.reason())
        );
        return new ReturnShipmentResult(
            saved.shipmentId(),
            saved.tenantId(),
            saved.passportId(),
            saved.shipmentRound(),
            saved.status().name(),
            saved.returnedAt(),
            saved.returnedByUserId(),
            saved.returnEvidenceGroupId(),
            outboxEventId
        );
    }

}
