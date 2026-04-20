package io.attestry.workflow.application.shipment.command;

import static io.attestry.workflow.domain.WorkflowValidation.requireText;

import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort.EvidenceRecord;
import io.attestry.workflow.application.port.common.WorkflowLedgerOutboxPort;
import io.attestry.workflow.application.port.shipment.ShipmentProductReadPort;
import io.attestry.workflow.application.event.WorkflowLedgerEvents;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import io.attestry.workflow.domain.shipment.model.Shipment;
import io.attestry.workflow.domain.shipment.policy.ShipmentReleasePolicy;
import io.attestry.workflow.domain.shipment.policy.ShipmentReleasePolicy.ShipmentReleaseContext;
import io.attestry.workflow.domain.shipment.policy.ShipmentReturnPolicy;
import io.attestry.workflow.domain.shipment.policy.ShipmentReturnPolicy.ShipmentReturnContext;
import io.attestry.workflow.domain.shipment.repository.ShipmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
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
    private final ShipmentReturnPolicy returnPolicy;
    private final UserReadPort userReadPort;
    private final Clock clock;


    @Override
    @Transactional
    public ReleaseShipmentResult release(
        WorkflowActorContext principal,
        String passportId,
        ReleaseShipmentCommand command
    ) {
        String tenantId = principal.tenantId();
        assertReleaseAccess(principal, tenantId, passportId);

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
        List<String> evidenceHashes = loadReadyEvidenceHashes(evidenceGroupId, tenantId, "At least one READY evidence is required");

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

        String releasedByEmail = resolveUserEmail(saved.releasedByUserId());
        List<Map<String, Object>> evidenceDetails = loadReadyEvidenceDetails(evidenceGroupId);

        String outboxEventId = shipmentLedgerOutboxPort.enqueue(
            WorkflowLedgerEvents.shipmentReleased(saved, evidenceHashes, releasedByEmail, evidenceDetails)
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
        WorkflowActorContext principal,
        String shipmentId,
        ReturnShipmentCommand command
    ) {
        String tenantId = principal.tenantId();
        assertReturnAccess(principal, tenantId, shipmentId);
        Shipment current = shipmentRepository.findByShipmentId(shipmentId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "Shipment not found"));
        returnPolicy.assertReturnable(new ShipmentReturnContext(tenantId, current));

        String returnEvidenceGroupId = null;
        List<String> returnEvidenceHashes = List.of();
        if (command.returnEvidenceGroupId() != null && !command.returnEvidenceGroupId().isBlank()) {
            returnEvidenceGroupId = command.returnEvidenceGroupId().trim();
            returnEvidenceHashes = loadReadyEvidenceHashes(
                returnEvidenceGroupId,
                tenantId,
                "returnEvidenceGroupId has no READY evidences"
            );
        }

        Instant now = Instant.now(clock);
        Shipment returned = current.markReturned(principal.userId(), returnEvidenceGroupId, now);
        Shipment saved = shipmentRepository.saveReturn(returned);

        String releasedByEmail = resolveUserEmail(saved.releasedByUserId());
        String returnedByEmail = resolveUserEmail(saved.returnedByUserId());
        List<Map<String, Object>> evidenceDetails = loadAllEvidenceDetails(saved.evidenceGroupId(), saved.returnEvidenceGroupId());

        String outboxEventId = shipmentLedgerOutboxPort.enqueue(
            WorkflowLedgerEvents.shipmentReturned(saved, returnEvidenceHashes, command.reason(),
                releasedByEmail, returnedByEmail, evidenceDetails)
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

    private String resolveUserEmail(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        Map<String, String> emails = userReadPort.findEmailMapByUserIds(List.of(userId));
        return emails.get(userId);
    }

    private List<Map<String, Object>> loadReadyEvidenceDetails(String evidenceGroupId) {
        if (evidenceGroupId == null) {
            return List.of();
        }
        return evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(e -> "READY".equals(e.status()))
            .map(this::toEvidenceMap)
            .toList();
    }

    private List<Map<String, Object>> loadAllEvidenceDetails(String evidenceGroupId, String returnEvidenceGroupId) {
        List<EvidenceRecord> all = new ArrayList<>();
        if (evidenceGroupId != null) {
            all.addAll(evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
                .filter(e -> "READY".equals(e.status())).toList());
        }
        if (returnEvidenceGroupId != null) {
            all.addAll(evidencePort.findEvidenceByEvidenceGroupId(returnEvidenceGroupId).stream()
                .filter(e -> "READY".equals(e.status())).toList());
        }
        return all.stream().map(this::toEvidenceMap).toList();
    }

    private Map<String, Object> toEvidenceMap(EvidenceRecord e) {
        Map<String, Object> map = new HashMap<>();
        map.put("evidenceId", e.evidenceId());
        map.put("originalFileName", e.originalFileName() == null ? "" : e.originalFileName());
        map.put("contentType", e.contentType() == null ? "application/octet-stream" : e.contentType());
        map.put("sizeBytes", e.sizeBytes());
        map.put("objectKey", e.objectKey() == null ? "" : e.objectKey());
        return map;
    }

    private void assertReleaseAccess(WorkflowActorContext principal, String tenantId, String passportId) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(
            principal,
            tenantId,
            PermissionCodes.BRAND_RELEASE,
            "shipment:release:" + passportId
        );
    }

    private void assertReturnAccess(WorkflowActorContext principal, String tenantId, String shipmentId) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(
            principal,
            tenantId,
            PermissionCodes.BRAND_RELEASE,
            "shipment:return:" + shipmentId
        );
    }

    private List<String> loadReadyEvidenceHashes(String evidenceGroupId, String tenantId, String emptyMessage) {
        evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, evidenceGroupId, tenantId);
        List<String> evidenceHashes = evidencePort.findReadyEvidenceHashes(evidenceGroupId);
        if (evidenceHashes.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, emptyMessage);
        }
        return evidenceHashes;
    }

}
