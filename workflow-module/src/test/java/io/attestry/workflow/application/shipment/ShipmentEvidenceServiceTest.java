package io.attestry.workflow.application.shipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.commonlib.application.port.ObjectStoragePort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.shipment.command.ShipmentEvidenceService;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.shipment.command.CompleteShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.command.PresignShipmentEvidenceUploadCommand;
import io.attestry.workflow.application.shipment.result.EvidenceCompleteResult;
import io.attestry.workflow.application.shipment.result.PresignedEvidenceUploadResult;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentEvidenceServiceTest {

    @Mock WorkflowEvidencePort evidencePort;
    @Mock ObjectStoragePort objectStoragePort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;
    @Mock EvidenceUploadSupport evidenceUploadSupport;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-12T03:00:00Z"), ZoneOffset.UTC);

    private ShipmentEvidenceService service;

    private static final WorkflowActorContext PRINCIPAL = new WorkflowActorContext(
        "token-1",
        "user-1",
        "tenant-1",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_BRAND_RELEASE"),
        Instant.parse("2026-03-13T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new ShipmentEvidenceService(
            evidencePort,
            objectStoragePort,
            authorizationSupport,
            evidenceUploadSupport,
            clock
        );
    }

    @Test
    void presignEvidenceUpload_checksAuthorizationAndDelegatesToSupport() {
        PresignedEvidenceUploadResult expected = new PresignedEvidenceUploadResult(
            "group-1",
            "evidence-1",
            "workflow/shipment/tenant-1/group-1/file.jpg",
            "https://upload",
            Instant.parse("2026-03-12T03:15:00Z")
        );
        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "tenant-1");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "tenant-1", PermissionCodes.BRAND_RELEASE, "shipment:evidence:presign"
        );
        when(evidenceUploadSupport.doPresign(
            evidencePort,
            objectStoragePort,
            "workflow/shipment/",
            java.time.Duration.ofMinutes(15),
            "tenant-1",
            "user-1",
            "group-1",
            "file.jpg",
            "image/jpeg",
            Instant.parse("2026-03-12T03:00:00Z")
        )).thenReturn(expected);

        PresignedEvidenceUploadResult result = service.presignEvidenceUpload(
            PRINCIPAL,
            new PresignShipmentEvidenceUploadCommand("group-1", "file.jpg", "image/jpeg")
        );

        assertEquals(expected, result);
    }

    @Test
    void completeEvidenceUpload_checksScopeBeforeCompletion() {
        EvidenceCompleteResult expected = new EvidenceCompleteResult("group-1", "evidence-1", "READY");
        doNothing().when(authorizationSupport).assertTenantContext(PRINCIPAL, "tenant-1");
        doNothing().when(authorizationSupport).assertLivePermission(
            PRINCIPAL, "tenant-1", PermissionCodes.BRAND_RELEASE, "shipment:evidence:complete"
        );
        doNothing().when(evidenceUploadSupport).assertEvidenceGroupScope(evidencePort, "group-1", "tenant-1");
        when(evidenceUploadSupport.doComplete(
            evidencePort,
            objectStoragePort,
            "group-1",
            "evidence-1",
            42L,
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            Instant.parse("2026-03-12T03:00:00Z")
        )).thenReturn(expected);

        EvidenceCompleteResult result = service.completeEvidenceUpload(
            PRINCIPAL,
            new CompleteShipmentEvidenceUploadCommand(
                "group-1",
                "evidence-1",
                42L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            )
        );

        assertEquals(expected, result);
        verify(evidenceUploadSupport).assertEvidenceGroupScope(evidencePort, "group-1", "tenant-1");
    }
}
