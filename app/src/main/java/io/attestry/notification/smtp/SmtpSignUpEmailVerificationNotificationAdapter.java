package io.attestry.notification.smtp;

import io.attestry.notification.SignUpVerificationNotificationProperties;
import io.attestry.userauth.application.port.notification.SignUpEmailVerificationNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.user-auth.signup-email-verification.mail",
    name = "provider",
    havingValue = "SMTP"
)
public class SmtpSignUpEmailVerificationNotificationAdapter implements SignUpEmailVerificationNotificationPort {

    private final JavaMailSender mailSender;
    private final SignUpVerificationNotificationProperties properties;

    @Override
    public void send(SignUpEmailVerificationNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }

        mailSender.send(SmtpMailHelper.createMessage(
            mailSender,
            properties.getMail().getFrom(),
            notification.email(),
            "[Proveny] 회원가입 이메일 인증 코드",
            """
                Proveny 회원가입 인증 코드입니다.

                인증 코드: %s
                유효시간: %d초

                본인이 요청하지 않았다면 이 메일을 무시해주세요.
                """.formatted(notification.code(), notification.expiresInSeconds()),
            notification.dedupeKey()
        ));
    }
}
