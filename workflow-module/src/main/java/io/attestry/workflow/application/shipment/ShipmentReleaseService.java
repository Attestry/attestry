package io.attestry.workflow.application.shipment;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.ShipmentEvidencePort;
import io.attestry.workflow.application.port.ShipmentLedgerOutboxPort;
import io.attestry.workflow.application.port.ShipmentProductReadPort;
import io.attestry.workflow.application.shipment.command.ReleaseShipmentCommand;
import io.attestry.workflow.application.shipment.command.ReturnShipmentCommand;
import io.attestry.workflow.application.shipment.result.ReleaseShipmentResult;
import io.attestry.workflow.application.shipment.result.ReturnShipmentResult;
import io.attestry.workflow.application.shipment.result.WorkflowLedgerEventEnvelope;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentReleaseService implements ShipmentReleaseUseCase {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEvidencePort shipmentEvidencePort;
    private final ShipmentProductReadPort shipmentProductReadPort;
    private final ShipmentLedgerOutboxPort shipmentLedgerOutboxPort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final ShipmentReleasePolicy releasePolicy;
    private final Clock clock;

    public ShipmentReleaseService(
        ShipmentRepository shipmentRepository,
        ShipmentEvidencePort shipmentEvidencePort,
        ShipmentProductReadPort shipmentProductReadPort,
        ShipmentLedgerOutboxPort shipmentLedgerOutboxPort,
        WorkflowAuthorizationSupport authorizationSupport,
        ShipmentReleasePolicy releasePolicy,
        Clock clock
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEvidencePort = shipmentEvidencePort;
        this.shipmentProductReadPort = shipmentProductReadPort;
        this.shipmentLedgerOutboxPort = shipmentLedgerOutboxPort;
        this.authorizationSupport = authorizationSupport;
        this.releasePolicy = releasePolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReleaseShipmentResult release(
        AuthPrincipal principal,
        String tenantId,
        String groupId,
        String passportId,
        ReleaseShipmentCommand command
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:release:" + passportId);

        ShipmentProductReadPort.PassportState state = shipmentProductReadPort.findPassportState(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Passport not found"));
        ShipmentReleaseContext context = new ShipmentReleaseContext(
            tenantId, groupId,
            state.tenantId(), state.groupId(),
            state.assetState(), state.riskFlag(),
            shipmentRepository.existsActiveReleasedByPassportId(passportId)
        );
        releasePolicy.assertReleasable(context);

        String evidenceGroupId = requireText(command.evidenceGroupId(), "evidenceGroupId");
        assertEvidenceGroupScope(evidenceGroupId, tenantId, groupId);
        List<String> evidenceHashes = shipmentEvidencePort.findReadyEvidenceHashes(evidenceGroupId);
        if (evidenceHashes.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "At least one READY evidence is required");
        }

        Instant now = Instant.now(clock);
        Shipment shipment = Shipment.release(
            UUID.randomUUID().toString(),
            tenantId,
            groupId,
            passportId,
            shipmentRepository.nextShipmentRound(passportId),
            now,
            principal.userId(),
            principal.groupId(),
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
            saved.groupId(),
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
        String tenantId,
        String groupId,
        String shipmentId,
        ReturnShipmentCommand command
    ) {
        authorizationSupport.assertTenantAndGroupContext(principal, tenantId, groupId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "shipment:return:" + shipmentId);
        Shipment current = shipmentRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Shipment not found"));
        if (!tenantId.equals(current.tenantId()) || !groupId.equals(current.groupId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant/group shipment access denied");
        }
        if (current.status() != ShipmentStatus.RELEASED) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_STATE, "Only RELEASED shipment can be returned");
        }

        String returnEvidenceGroupId = null;
        List<String> returnEvidenceHashes = List.of();
        if (command.returnEvidenceGroupId() != null && !command.returnEvidenceGroupId().isBlank()) {
            returnEvidenceGroupId = command.returnEvidenceGroupId().trim();
            assertEvidenceGroupScope(returnEvidenceGroupId, tenantId, groupId);
            returnEvidenceHashes = shipmentEvidencePort.findReadyEvidenceHashes(returnEvidenceGroupId);
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
            saved.groupId(),
            saved.passportId(),
            saved.shipmentRound(),
            saved.status().name(),
            saved.returnedAt(),
            saved.returnedByUserId(),
            saved.returnEvidenceGroupId(),
            outboxEventId
        );
    }

    private void assertEvidenceGroupScope(String evidenceGroupId, String tenantId, String groupId) {
        ShipmentEvidencePort.EvidenceGroupScopeView scope = shipmentEvidencePort.findEvidenceGroupScope(evidenceGroupId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.EVIDENCE_NOT_FOUND, "Evidence group not found"));
        if (!tenantId.equals(scope.tenantId()) || !groupId.equals(scope.groupId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant/group evidence group access denied");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }
}
