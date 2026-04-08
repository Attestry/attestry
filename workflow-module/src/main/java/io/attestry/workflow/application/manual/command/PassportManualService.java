package io.attestry.workflow.application.manual.command;

import io.attestry.userauth.application.port.notification.NotificationOutboxWritePort;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload.AttachmentPayload;
import io.attestry.workflow.application.common.WorkflowActorContext;
import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;
import io.attestry.workflow.application.manual.result.SendPassportManualResult;
import io.attestry.workflow.application.manual.internal.PassportManualAttachmentResolver;
import io.attestry.workflow.application.manual.internal.PassportManualContextAccessService;
import io.attestry.workflow.application.manual.internal.PassportManualNotificationFactory;
import io.attestry.workflow.application.manual.internal.PassportManualRecipientResolver;
import io.attestry.workflow.application.port.manual.PassportManualReadPort;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassportManualService implements PassportManualUseCase {

    private static final String RECIPIENT_PERMISSION_PREFIX = "passport-manual:recipient:";
    private static final String SEND_PERMISSION_PREFIX = "passport-manual:send:";

    private final PassportManualContextAccessService contextAccessService;
    private final PassportManualRecipientResolver recipientResolver;
    private final PassportManualAttachmentResolver attachmentResolver;
    private final PassportManualNotificationFactory notificationFactory;
    private final NotificationOutboxWritePort notificationOutboxRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public PassportManualRecipientResult getRecipient(WorkflowActorContext principal, String tenantId, String passportId) {
        PassportManualReadPort.PassportManualContext context = contextAccessService.loadAuthorizedContext(
            principal,
            tenantId,
            passportId,
            RECIPIENT_PERMISSION_PREFIX
        );

        if (context.ownerUserId() == null || context.ownerUserId().isBlank()) {
            return new PassportManualRecipientResult(false, "현재 소유주가 없습니다.", null);
        }

        return new PassportManualRecipientResult(
            true,
            null,
            recipientResolver.maskEmail(recipientResolver.resolveRecipientEmail(context.ownerUserId()))
        );
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
            : attachmentResolver.resolve(evidenceGroupId, tenantId);
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
        PassportManualReadPort.PassportManualContext context = contextAccessService.loadAuthorizedContext(
            principal,
            tenantId,
            passportId,
            SEND_PERMISSION_PREFIX
        );
        if (context.ownerUserId() == null || context.ownerUserId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.PASSPORT_MANUAL_OWNER_NOT_FOUND, "현재 소유주가 없습니다.");
        }

        String recipientEmail = recipientResolver.resolveRecipientEmail(context.ownerUserId());
        notificationOutboxRepositoryPort.save(notificationFactory.create(
            context,
            recipientEmail,
            content.message(),
            content.evidenceGroupId(),
            content.attachments()
        ));
        return new SendPassportManualResult.PassportManualDeliveryResult(
            context.passportId(),
            recipientResolver.maskEmail(recipientEmail)
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
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
