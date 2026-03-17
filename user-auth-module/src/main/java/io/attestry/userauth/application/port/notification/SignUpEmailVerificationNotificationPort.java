package io.attestry.userauth.application.port.notification;

public interface SignUpEmailVerificationNotificationPort {

    void send(SignUpEmailVerificationNotification notification);

    record SignUpEmailVerificationNotification(
        String verificationId,
        String email,
        String code,
        long expiresInSeconds
    ) {
    }
}
