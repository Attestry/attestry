package io.attestry.workflow.application.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.attestry.userauth.application.port.notification.NotificationOutboxRepositoryPort;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.manual.command.SendPassportManualCommand;
import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;
import io.attestry.workflow.application.manual.result.SendPassportManualResult;
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
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PassportManualServiceTest {

    @Mock PassportManualReadPort passportManualReadPort;
    @Mock UserReadPort userReadPort;
    @Mock WorkflowEvidencePort workflowEvidencePort;
    @Mock WorkflowAuthorizationSupport authorizationSupport;
    @Mock NotificationOutboxRepositoryPort notificationOutboxRepositoryPort;

    private PassportManualService service;

    private static final AuthPrincipal BRAND = new AuthPrincipal(
        "token-1",
        "brand-user",
        "tenant-1",
        VerificationLevel.PHONE_VERIFIED,
        Set.of("SCOPE_BRAND_RELEASE"),
        Instant.parse("2026-03-18T00:00:00Z")
    );

    @BeforeEach
    void setUp() {
        service = new PassportManualService(
            passportManualReadPort,
            userReadPort,
            workflowEvidencePort,
            authorizationSupport,
            new EvidenceUploadSupport(),
            notificationOutboxRepositoryPort,
            Clock.fixed(Instant.parse("2026-03-17T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void getRecipient_returnsUnavailableWhenOwnerMissing() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
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
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", "owner-1"
            )));
        when(userReadPort.findEmailsByUserIds(List.of("owner-1")))
            .thenReturn(Map.of("owner-1", "owner@example.com"));

        SendPassportManualResult result = service.send(
            BRAND,
            "tenant-1",
            "passport-1",
            new SendPassportManualCommand("사용 설명입니다.", null)
        );

        assertEquals("passport-1", result.passportId());
        assertEquals(false, result.hasAttachment());
        assertEquals("o***@example.com", result.recipientEmailMasked());

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
        assertEquals(List.of(), payload.attachmentEvidenceIds());
    }

    @Test
    void send_queuesOutboxWhenAttachmentExists() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", "owner-1"
            )));
        when(userReadPort.findEmailsByUserIds(List.of("owner-1")))
            .thenReturn(Map.of("owner-1", "owner@example.com"));
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
            "passport-1",
            new SendPassportManualCommand("   ", " group-1 ")
        );

        assertEquals(true, result.hasAttachment());
        assertEquals("group-1", result.evidenceGroupId());

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(notificationOutboxRepositoryPort).save(captor.capture());
        PassportManualNotificationPayload payload = assertInstanceOf(
            PassportManualNotificationPayload.class,
            captor.getValue().payload()
        );
        assertEquals(null, payload.message());
        assertEquals("group-1", payload.evidenceGroupId());
        assertEquals(List.of("evidence-1"), payload.attachmentEvidenceIds());
    }

    @Test
    void send_throwsWhenNeitherMessageNorAttachmentExists() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", "owner-1"
            )));

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", "passport-1", new SendPassportManualCommand("   ", null))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED, exception.getErrorCode());
    }

    @Test
    void send_throwsWhenOwnerMissing() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", null
            )));

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", "passport-1", new SendPassportManualCommand("사용 설명입니다.", null))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_OWNER_NOT_FOUND, exception.getErrorCode());
        verify(notificationOutboxRepositoryPort, never()).save(any(NotificationOutbox.class));
    }

    @Test
    void send_throwsWhenRecipientEmailMissing() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", "owner-1"
            )));
        when(userReadPort.findEmailsByUserIds(List.of("owner-1")))
            .thenReturn(Map.of());

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", "passport-1", new SendPassportManualCommand("사용 설명입니다.", null))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_RECIPIENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void send_throwsWhenAttachmentHasNoReadyEvidence() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", "owner-1"
            )));
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
            service.send(BRAND, "tenant-1", "passport-1", new SendPassportManualCommand(null, "group-1"))
        );

        assertEquals(WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED, exception.getErrorCode());
        verify(notificationOutboxRepositoryPort, never()).save(any(NotificationOutbox.class));
    }

    @Test
    void send_throwsWhenAttachmentScopeBelongsToDifferentTenant() {
        doNothing().when(authorizationSupport).assertTenantContext(any(), anyString());
        doNothing().when(authorizationSupport).assertLivePermission(any(), anyString(), anyString(), anyString());
        when(passportManualReadPort.findContext("passport-1"))
            .thenReturn(Optional.of(new PassportManualReadPort.PassportManualContext(
                "passport-1", "tenant-1", "SN-1", "Model A", "owner-1"
            )));
        when(workflowEvidencePort.findEvidenceGroupScope("group-1"))
            .thenReturn(Optional.of(new WorkflowEvidencePort.EvidenceGroupScopeRecord("group-1", "tenant-2", "brand-user")));

        WorkflowDomainException exception = assertThrows(WorkflowDomainException.class, () ->
            service.send(BRAND, "tenant-1", "passport-1", new SendPassportManualCommand(null, "group-1"))
        );

        assertEquals(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, exception.getErrorCode());
        verify(notificationOutboxRepositoryPort, never()).save(any(NotificationOutbox.class));
    }
}
