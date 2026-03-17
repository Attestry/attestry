package io.attestry.notification;

import io.attestry.userauth.application.port.notification.SignUpEmailVerificationNotificationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "app.user-auth.signup-email-verification.mail",
    name = "provider",
    havingValue = "SMTP"
)
public class SmtpSignUpEmailVerificationNotificationAdapter implements SignUpEmailVerificationNotificationPort {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;

    public SmtpSignUpEmailVerificationNotificationAdapter(
        JavaMailSender mailSender,
        @Value("${app.user-auth.signup-email-verification.mail.enabled:true}") boolean enabled,
        @Value("${app.user-auth.signup-email-verification.mail.from:no-reply@attestry.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(SignUpEmailVerificationNotification notification) {
        if (!enabled) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(notification.email());
        message.setSubject("[Proveny] 회원가입 이메일 인증 코드");
        message.setText("""
            Proveny 회원가입 인증 코드입니다.

            인증 코드: %s
            유효시간: %d초

            본인이 요청하지 않았다면 이 메일을 무시해주세요.
            """.formatted(notification.code(), notification.expiresInSeconds()));

        mailSender.send(message);
    }
}
