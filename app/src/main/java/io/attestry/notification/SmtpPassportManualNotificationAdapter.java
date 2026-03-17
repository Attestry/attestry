package io.attestry.notification;

import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "app.workflow.passport-manual.mail",
    name = "provider",
    havingValue = "SMTP"
)
public class SmtpPassportManualNotificationAdapter implements PassportManualNotificationPort {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;
    private final PassportManualAttachmentResolver attachmentResolver;

    public SmtpPassportManualNotificationAdapter(
        JavaMailSender mailSender,
        @Value("${app.workflow.passport-manual.mail.enabled:true}") boolean enabled,
        @Value("${app.workflow.passport-manual.mail.from:no-reply@attestry.local}") String fromAddress,
        PassportManualAttachmentResolver attachmentResolver
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.attachmentResolver = attachmentResolver;
    }

    @Override
    public void send(PassportManualNotification notification) {
        if (!enabled) {
            return;
        }

        List<ManualAttachment> attachments = attachmentResolver.resolveAttachments(notification);
        String attachmentText = attachments.isEmpty()
            ? ""
            : "\n첨부 자료:\n" + attachments.stream()
                .map(attachment -> "- " + attachment.fileName() + ": " + attachment.downloadUrl())
                .collect(Collectors.joining("\n"));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(notification.recipientEmail());
        message.setSubject("[Proveny] 제품 메뉴얼이 도착했습니다");
        message.setText("""
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
        ));

        mailSender.send(message);
    }
}
