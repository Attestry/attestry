package io.attestry.workflow.application.manual;

import io.attestry.userauth.application.port.notification.NotificationOutboxRepositoryPort;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
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
import io.attestry.workflow.application.usecase.PassportManualUseCase;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassportManualService implements PassportManualUseCase {

    private final PassportManualReadPort passportManualReadPort;
    private final UserReadPort userReadPort;
    private final WorkflowEvidencePort evidencePort;
    private final WorkflowAuthorizationSupport authorizationSupport;
    private final EvidenceUploadSupport evidenceUploadSupport;
    private final NotificationOutboxRepositoryPort notificationOutboxRepositoryPort;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PassportManualRecipientResult getRecipient(AuthPrincipal principal, String tenantId, String passportId) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "passport-manual:recipient:" + passportId);

        PassportManualReadPort.PassportManualContext context = loadContext(passportId);
        assertTenantPassport(tenantId, context);

        if (context.ownerUserId() == null || context.ownerUserId().isBlank()) {
            return new PassportManualRecipientResult(false, "현재 소유주가 없습니다.", null);
        }

        String email = findRecipientEmail(context.ownerUserId());
        return new PassportManualRecipientResult(true, null, maskEmail(email));
    }

    @Override
    @Transactional
    public SendPassportManualResult send(
        AuthPrincipal principal,
        String tenantId,
        String passportId,
        SendPassportManualCommand command
    ) {
        authorizationSupport.assertTenantContext(principal, tenantId);
        authorizationSupport.assertLivePermission(principal, tenantId, PermissionCodes.BRAND_RELEASE, "passport-manual:send:" + passportId);

        PassportManualReadPort.PassportManualContext context = loadContext(passportId);
        assertTenantPassport(tenantId, context);

        if (context.ownerUserId() == null || context.ownerUserId().isBlank()) {
            throw new WorkflowDomainException(WorkflowErrorCode.PASSPORT_MANUAL_OWNER_NOT_FOUND, "현재 소유주가 없습니다.");
        }

        String trimmedMessage = command.message() == null ? null : command.message().trim();
        String evidenceGroupId = command.evidenceGroupId() == null ? null : command.evidenceGroupId().trim();
        boolean hasMessage = trimmedMessage != null && !trimmedMessage.isBlank();
        boolean hasAttachment = evidenceGroupId != null && !evidenceGroupId.isBlank();

        if (!hasMessage && !hasAttachment) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED,
                "메뉴얼 내용을 입력하거나 첨부 파일을 추가해주세요."
            );
        }

        List<String> attachmentEvidenceIds = List.of();
        if (hasAttachment) {
            evidenceUploadSupport.assertEvidenceGroupScope(evidencePort, evidenceGroupId, tenantId);
            attachmentEvidenceIds = evidencePort.findEvidenceByEvidenceGroupId(evidenceGroupId).stream()
                .filter(evidence -> "READY".equalsIgnoreCase(evidence.status()))
                .map(WorkflowEvidencePort.EvidenceRecord::evidenceId)
                .filter(Objects::nonNull)
                .toList();
            if (attachmentEvidenceIds.isEmpty()) {
                throw new WorkflowDomainException(
                    WorkflowErrorCode.PASSPORT_MANUAL_CONTENT_REQUIRED,
                    "첨부 파일을 다시 확인해주세요."
                );
            }
        }

        String recipientEmail = findRecipientEmail(context.ownerUserId());
        NotificationOutbox outbox = NotificationOutbox.create(
            NotificationType.PASSPORT_MANUAL_DELIVERY,
            recipientEmail,
            new PassportManualNotificationPayload(
                context.passportId(),
                context.tenantId(),
                recipientEmail,
                context.serialNumber(),
                context.modelName(),
                trimmedMessage,
                evidenceGroupId,
                attachmentEvidenceIds
            ),
            Instant.now(clock)
        );
        notificationOutboxRepositoryPort.save(outbox);

        return new SendPassportManualResult(
            context.passportId(),
            maskEmail(recipientEmail),
            evidenceGroupId,
            hasAttachment
        );
    }

    private PassportManualReadPort.PassportManualContext loadContext(String passportId) {
        return passportManualReadPort.findContext(passportId)
            .orElseThrow(() -> new WorkflowDomainException(WorkflowErrorCode.INVALID_REQUEST, "제품 정보를 찾을 수 없습니다."));
    }

    private void assertTenantPassport(String tenantId, PassportManualReadPort.PassportManualContext context) {
        if (!tenantId.equals(context.tenantId())) {
            throw new WorkflowDomainException(WorkflowErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    private String findRecipientEmail(String ownerUserId) {
        Map<String, String> emailMap = userReadPort.findEmailsByUserIds(List.of(ownerUserId));
        String email = emailMap.get(ownerUserId);
        if (email == null || email.isBlank()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PASSPORT_MANUAL_RECIPIENT_NOT_FOUND,
                "현재 소유자의 이메일 정보를 확인할 수 없습니다."
            );
        }
        return email;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
