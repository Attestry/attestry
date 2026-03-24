package io.attestry.notification.smtp;

import io.attestry.notification.PassportManualAttachmentResolver;
import io.attestry.notification.PassportManualNotificationProperties;
import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.workflow.passport-manual.mail",
    name = "provider",
    havingValue = "SMTP"
)
public class SmtpPassportManualNotificationAdapter implements PassportManualNotificationPort {

    private final JavaMailSender mailSender;
    private final PassportManualNotificationProperties properties;
    private final PassportManualAttachmentResolver attachmentResolver;

    @Override
    public void send(PassportManualNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }

        List<ManualAttachment> attachments = attachmentResolver.resolveAttachments(notification);
        String attachmentText = attachments.isEmpty()
            ? ""
            : "\n첨부 자료:\n" + attachments.stream()
                .map(a -> "- " + a.fileName() + ": " + a.downloadUrl())
                .collect(Collectors.joining("\n"));

        mailSender.send(SmtpMailHelper.createMessage(
            mailSender,
            properties.getMail().getFrom(),
            notification.recipientEmail(),
            "[Proveny] 제품 메뉴얼이 도착했습니다",
            """
                제품 메뉴얼이 전달되었습니다.

                모델명: %s
                시리얼 번호: %s

                전달 내용:
                %s
                %s

                본 메일은 Proveny를 통해 발송되었습니다.
                """.formatted(
                notification.modelName(),
                notification.serialNumber(),
                notification.message() == null || notification.message().isBlank() ? "-" : notification.message(),
                attachmentText
            ),
            notification.dedupeKey()
        ));
    }
}
