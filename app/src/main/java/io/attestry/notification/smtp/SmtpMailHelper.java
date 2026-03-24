package io.attestry.notification.smtp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

final class SmtpMailHelper {

    private SmtpMailHelper() {
    }

    static MimeMessage createMessage(
        JavaMailSender mailSender,
        String from,
        String to,
        String subject,
        String body,
        String dedupeKey
    ) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            message.setHeader("X-Attestry-Dedup-Key", dedupeKey);
            message.setHeader("Message-ID", toMessageId(dedupeKey));
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to build email message", ex);
        }
        return message;
    }

    private static String toMessageId(String dedupeKey) {
        return "<" + dedupeKey.replaceAll("[^A-Za-z0-9._-]", "_") + "@attestry.local>";
    }
}
