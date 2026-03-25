package io.attestry.workflow.application.manual.command;

import io.attestry.userauth.application.port.notification.NotificationOutboxWritePort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload.AttachmentPayload;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;
import io.attestry.workflow.application.manual.result.SendPassportManualResult;
import io.attestry.workflow.application.manual.usecase.PassportManualUseCase;
import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.application.port.common.WorkflowEvidencePort;
import io.attestry.workflow.application.port.manual.PassportManualReadPort;
import io.attestry.workflow.application.support.EvidenceUploadSupport;
import io.attestry.workflow.application.support.WorkflowAuthorizationSupport;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassportManualService implements PassportManualUseCase {

    private static final String READY_EVIDENCE_STATUS = "READY";
    private static final String RECIPIENT_PERMISSION_PREFIX = "passport-manual:recipient:";
    private static final String SEND_PERMISSION_PREFIX = "passport-manual:send:";

    private final PassportManualReadPort passportManualReadPort;
    private final UserReadPort userReadPort;
    private final WorkflowEvidencePort evidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final EvidenceUploadSupport evidenceUploadSupport;
    private final NotificationOutboxWritePort notificationOutboxRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PassportManualRecipientResult getRecipient(WorkflowActorContext principal, String tenantId, String passportId) {
        PassportManualReadPort.PassportManualContext context = loadAuthorizedContext(
            principal,
            tenantId,
            passportId,
            RECIPIENT_PERMISSION_PREFIX
        );

        if (context.ownerUserId() == null || context.ownerUserId().isBlank()) {
            return new PassportManualRecipientResult(false, "현재 소유주가 없습니다.", null);
        }

        return new PassportManualRecipientResult(true, null, maskEmail(resolveRecipientEmail(context.ownerUserId())));
    }

    @Override
    @Transactional
    public SendPassportManualResult send(
        WorkflowActorContext principal,
        String tenantId,
        SendPassportManualCommand command
    ) {
        List<String> passportIds = normalizePassportIds(command.passportIds());
        ManualContent content = resolveManualContent(command, tenantId);
        List<SendPassportManualResult.PassportManualDeliveryResult> deliveries = passportIds.stream()
            .map(passportId -> queuePassportManualDelivery(principal, tenantId, passportId, content))
            .toList();

        return new SendPassportManualResult(
            deliveries.size(),
            content.hasAttachment(),
            content.evidenceGroupId(),
            deliveries
        );
    }

    private PassportManualReadPort.PassportManualContext loadAuthorizedContext(
        WorkflowActorContext principal,
        String tenantId,
        String passportId,
        String permissionActionPrefix
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(
            principal,
            tenantId,
            PermissionCodes.BRAND_RELEASE,
            permissionActionPrefix + passportId
        );

        PassportManualReadPort.PassportManualContext context = passportManualReadPort.findContext(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "제품 정보를 찾을 수 없습니다."));
        if (!tenantId.equals(context.tenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
        return context;
    }

    private ManualContent resolveManualContent(SendPassportManualCommand command, String tenantId) {
        String message = trimToNull(command.message());
        String evidenceGroupId = trimToNull(command.evidenceGroupId());
        if (message == null && evidenceGroupId == null) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED,
                "메뉴얼 내용을 입력하거나 첨부 파일을 추가해주세요."
            );
        }

        List<AttachmentPayload> attachments = evidenceGroupId == null
            ? List.of()
            : resolveAttachments(evidenceGroupId, tenantId);
        return new ManualContent(message, evidenceGroupId, attachments);
    }

    private List<String> normalizePassportIds(List<String> passportIds) {
        if (passportIds == null || passportIds.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "passportIds is required");
        }
        List<String> normalizedPassportIds = passportIds.stream()
            .map(this::trimToNull)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (normalizedPassportIds.isEmpty()) {
            throw new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "passportIds is required");
        }
        return normalizedPassportIds;
    }

    private SendPassportManualResult.PassportManualDeliveryResult queuePassportManualDelivery(
        WorkflowActorContext principal,
        String tenantId,
        String passportId,
        ManualContent content
    ) {
        PassportManualReadPort.PassportManualContext context = loadAuthorizedContext(
            principal,
            tenantId,
            passportId,
            SEND_PERMISSION_PREFIX
        );
        if (context.ownerUserId() == null || context.ownerUserId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.PASSPORT_MANUAL_OWNER_NOT_FOUND, "현재 소유주가 없습니다.");
        }

        String recipientEmail = resolveRecipientEmail(context.ownerUserId());
        notificationOutboxRepositoryPort.save(buildOutbox(context, recipientEmail, content));
        return new SendPassportManualResult.PassportManualDeliveryResult(
            context.passportId(),
            maskEmail(recipientEmail)
        );
    }

    private List<AttachmentPayload> resolveAttachments(String evidenceGroupId, String tenantId) {
        evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, evidenceGroupId, tenantId);
        List<AttachmentPayload> attachments = evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
            .filter(evidence -> READY_EVIDENCE_STATUS.equalsIgnoreCase(evidence.status()))
            .map(evidence -> new AttachmentPayload(
                evidence.evidenceId(),
                evidence.originalFileName(),
                evidence.objectKey(),
                evidence.contentType()
            ))
            .toList();
        if (attachments.isEmpty()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED,
                "첨부 파일을 다시 확인해주세요."
            );
        }
        return attachments;
    }

    private String resolveRecipientEmail(String ownerUserId) {
        Map<String, String> emailMap = userReadPort.findEmailMapByUserIds(List.of(ownerUserId));
        String email = emailMap.get(ownerUserId);
        if (email == null || email.isBlank()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PASSPORT_MANUAL_RECIPIENT_NOT_FOUND,
                "현재 소유자의 이메일 정보를 확인할 수 없습니다."
            );
        }
        return email;
    }

    private NotificationOutbox buildOutbox(
        PassportManualReadPort.PassportManualContext context,
        String recipientEmail,
        ManualContent content
    ) {
        return NotificationOutbox.create(
            NotificationType.PASSPORT_MANUAL_DELIVERY,
            recipientEmail,
            new PassportManualNotificationPayload(
                context.passportId(),
                context.tenantId(),
                recipientEmail,
                context.serialNumber(),
                context.modelName(),
                content.message(),
                content.evidenceGroupId(),
                content.attachments()
            ),
            Instant.now(clock)
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private record ManualContent(
        String message,
        String evidenceGroupId,
        List<AttachmentPayload> attachments
    ) {
        private boolean hasAttachment() {
            return evidenceGroupId != null;
        }
    }
}
