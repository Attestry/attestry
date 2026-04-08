package io.attestry.workflow.application.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.application.port.notification.NotificationOutboxWritePort;
import io.attestry.userauth.domain.auth.model.VerificationLevel;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.manual.command.PassportManualService;
import io.attestry.workflow.application.manual.command.SendPassportManualCommand;
import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;
import io.attestry.workflow.application.manual.result.SendPassportManualResult;
import io.attestry.workflow.application.manual.internal.PassportManualAttachmentResolver;
import io.attestry.workflow.application.manual.internal.PassportManualContextAccessService;
import io.attestry.workflow.application.manual.internal.PassportManualNotificationFactory;
import io.attestry.workflow.application.manual.internal.PassportManualRecipientResolver;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.manual.PassportManualReadPort;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassportManualServiceTest {

    @Mock PassportManualReadPort passportManualReadPort;
    @Mock UserReadPort userReadPort;
    @Mock WorkflowEvidencePort workflowEvidencePort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;
    @Mock NotificationOutboxWritePort notificationOutboxRepositoryPort;

    private PassportManualService service;

    private static final WorkflowActorContext BRAND = new WorkflowActorContext(
        "token-1",
        "brand-user",
        "tenant-1",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_BRAND_RELEASE"),
        Instant.parse("2026-03-18T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-17T10:00:00Z"), ZoneOffset.UTC);
        service = new PassportManualService(
            new PassportManualContextAccessService(passportManualReadPort, authorizationSupport),
            new PassportManualRecipientResolver(userReadPort),
            new PassportManualAttachmentResolver(workflowEvidencePort, new EvidenceUploadSupport()),
            new PassportManualNotificationFactory(clock),
            notificationOutboxRepositoryPort
        );
    }

    @Test
    void getRecipient_returnsUnavailableWhenOwnerMissing() {
        stubAuthorization();
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", null
            )));

        PassportManualRecipientResult result = service.getRecipient(BRAND, "tenant-1", "passport-1");

        assertEquals(false, result.available());
        assertEquals("현재 소유주가 없습니다.", result.message());
    }

    @Test
    void send_queuesOutboxWhenMessageOnly() {
        stubAuthorization();
        stubPassport("passport-1", "tenant-1", "SN-1", "Model A", "owner-1");
        when(userReadPort.findEmailMapByUserIds(List.of("owner-1")))
            .thenReturn(Map.of("owner-1", "owner@example.com"));

        SendPassportManualResult result = service.send(
            BRAND,
            "tenant-1",
            new SendPassportManualCommand(List.of("passport-1"), "사용 설명입니다.", null)
        );

        assertEquals(1, result.queuedCount());
        assertEquals(false, result.hasAttachment());
        assertEquals("passport-1", result.deliveries().getFirst().passportId());
        assertEquals("o***@example.com", result.deliveries().getFirst().recipientEmailMasked());

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(notificationOutboxRepositoryPort).save(captor.capture());
        NotificationOutbox savedOutbox = captor.getValue();
        assertEquals(NotificationType.PASSPORT_MANUAL_DELIVERY, savedOutbox.notificationType());
        PassportManualNotificationPayload payload = assertInstanceOf(
            PassportManualNotificationPayload.class,
            savedOutbox.payload()
        );
        assertEquals("passport-1", payload.passportId());
        assertEquals("tenant-1", payload.tenantId());
        assertEquals("owner@example.com", payload.recipientEmail());
        assertEquals("SN-1", payload.serialNumber());
        assertEquals("Model A", payload.modelName());
        assertEquals("사용 설명입니다.", payload.message());
        assertEquals(null, payload.evidenceGroupId());
        assertEquals(List.of(), payload.attachments());
    }

    @Test
    void send_queuesOutboxPerPassportWhenAttachmentExists() {
        stubAuthorization();
        stubPassport("passport-1", "tenant-1", "SN-1", "Model A", "owner-1");
        stubPassport("passport-2", "tenant-1", "SN-2", "Model B", "owner-2");
        when(userReadPort.findEmailMapByUserIds(List.of("owner-1")))
            .thenReturn(Map.of("owner-1", "owner@example.com"));
        when(userReadPort.findEmailMapByUserIds(List.of("owner-2")))
            .thenReturn(Map.of("owner-2", "second@example.com"));
        when(workflowEvidencePort.findEvidenceGroupScope("group-1"))
            .thenReturn(Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeRecord("group-1", "tenant-1", "brand-user")));
        when(workflowEvidencePort.findEvidenceByEvidenceGroupId("group-1"))
            .thenReturn(List.of(
                new WorkflowEvidencePort.EvidenceRecord(
                    "evidence-1",
                    "group-1",
                    "hash-1",
                    "passport-manual/test.pdf",
                    "test.pdf",
                    "application/pdf",
                    10L,
                    "READY"
                )
            ));

        SendPassportManualResult result = service.send(
            BRAND,
            "tenant-1",
            new SendPassportManualCommand(List.of("passport-1", "passport-2"), "   ", " group-1 ")
        );

        assertEquals(2, result.queuedCount());
        assertEquals(true, result.hasAttachment());
        assertEquals("group-1", result.evidenceGroupId());
        assertEquals(2, result.deliveries().size());
        verify(notificationOutboxRepositoryPort, times(2)).save(any(NotificationOutbox.class));
    }

    @Test
    void send_throwsWhenNeitherMessageNorAttachmentExists() {
        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", new SendPassportManualCommand(List.of("passport-1"), "   ", null))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED, exception.getErrorCode());
    }

    @Test
    void send_throwsWhenOwnerMissing() {
        stubAuthorization();
        stubPassport("passport-1", "tenant-1", "SN-1", "Model A", null);

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", new SendPassportManualCommand(List.of("passport-1"), "사용 설명입니다.", null))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_OWNER_NOT_FOUND, exception.getErrorCode());
        verify(notificationOutboxRepositoryPort, never()).save(any(NotificationOutbox.class));
    }

    @Test
    void send_throwsWhenRecipientEmailMissing() {
        stubAuthorization();
        stubPassport("passport-1", "tenant-1", "SN-1", "Model A", "owner-1");
        when(userReadPort.findEmailMapByUserIds(List.of("owner-1")))
            .thenReturn(Map.of());

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", new SendPassportManualCommand(List.of("passport-1"), "사용 설명입니다.", null))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_RECIPIENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void send_throwsWhenAttachmentHasNoReadyEvidence() {
        when(workflowEvidencePort.findEvidenceGroupScope("group-1"))
            .thenReturn(Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeRecord("group-1", "tenant-1", "brand-user")));
        when(workflowEvidencePort.findEvidenceByEvidenceGroupId("group-1"))
            .thenReturn(List.of(
                new WorkflowEvidencePort.EvidenceRecord(
                    "evidence-1",
                    "group-1",
                    "hash-1",
                    "passport-manual/test.pdf",
                    "test.pdf",
                    "application/pdf",
                    10L,
                    "PENDING_UPLOAD"
                )
            ));

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", new SendPassportManualCommand(List.of("passport-1"), null, "group-1"))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED, exception.getErrorCode());
        verify(notificationOutboxRepositoryPort, never()).save(any(NotificationOutbox.class));
    }

    @Test
    void send_throwsWhenAttachmentScopeBelongsToDifferentTenant() {
        when(workflowEvidencePort.findEvidenceGroupScope("group-1"))
            .thenReturn(Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeRecord("group-1", "tenant-2", "brand-user")));

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", new SendPassportManualCommand(List.of("passport-1"), null, "group-1"))
        );

        assertEquals(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, exception.getErrorCode());
        verify(notificationOutboxRepositoryPort, never()).save(any(NotificationOutbox.class));
    }

    @Test
    void send_throwsWhenPassportIdsEmpty() {
        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", new SendPassportManualCommand(List.of(), "사용 설명입니다.", null))
        );

        assertEquals(WorkflowErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    private void stubAuthorization() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
    }

    private void stubPassport(String passportId, String tenantId, String serialNumber, String modelName, String ownerUserId) {
        when(passportManualReadPort.findContext(passportId))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                passportId, tenantId, serialNumber, modelName, ownerUserId
            )));
    }
}
