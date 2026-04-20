package io.attestry.userauth.domain.auth.model;

import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class SignUpEmailVerification {

    private final String verificationId;
    private final String email;
    private String codeHash;
    private Instant expiresAt;
    private Instant verifiedAt;
    private Instant consumedAt;
    private int resendCount;
    private int confirmAttemptCount;
    private Instant lastSentAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private SignUpEmailVerification(
        String verificationId,
        String email,
        String codeHash,
        Instant expiresAt,
        Instant verifiedAt,
        Instant consumedAt,
        int resendCount,
        int confirmAttemptCount,
        Instant lastSentAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.verificationId = verificationId;
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.verifiedAt = verifiedAt;
        this.consumedAt = consumedAt;
        this.resendCount = resendCount;
        this.confirmAttemptCount = confirmAttemptCount;
        this.lastSentAt = lastSentAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SignUpEmailVerification issue(String email, String codeHash, Instant now, Duration ttl) {
        String normalizedEmail = Email.of(email).value();
        if (codeHash == null || codeHash.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "Verification code hash is required");
        }

        return new SignUpEmailVerification(
            UUID.randomUUID().toString(),
            normalizedEmail,
            codeHash,
            now.plus(ttl),
            null,
            null,
            0,
            0,
            now,
            now,
            now
        );
    }

    public static SignUpEmailVerification reconstitute(
        String verificationId,
        String email,
        String codeHash,
        Instant expiresAt,
        Instant verifiedAt,
        Instant consumedAt,
        int resendCount,
        int confirmAttemptCount,
        Instant lastSentAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new SignUpEmailVerification(
            verificationId,
            Email.of(email).value(),
            codeHash,
            expiresAt,
            verifiedAt,
            consumedAt,
            resendCount,
            confirmAttemptCount,
            lastSentAt,
            createdAt,
            updatedAt
        );
    }

    public void regenerate(String newCodeHash, Instant now, Duration ttl, Duration cooldown, int maxResends) {
        assertReusable(now);
        if (newCodeHash == null || newCodeHash.isBlank()) {
            throw new UserAuthDomainException(UserAuthErrorCode.INVALID_REQUEST, "Verification code hash is required");
        }
        if (lastSentAt != null && lastSentAt.plus(cooldown).isAfter(now)) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_RESEND_COOLDOWN,
                "Verification code resend cooldown active"
            );
        }
        if (resendCount >= maxResends) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_RESEND_LIMIT_EXCEEDED,
                "Verification code resend limit exceeded"
            );
        }

        this.codeHash = newCodeHash;
        this.expiresAt = now.plus(ttl);
        this.verifiedAt = null;
        this.resendCount += 1;
        this.confirmAttemptCount = 0;
        this.lastSentAt = now;
        this.updatedAt = now;
    }

    public void verify(String rawCode, CodeMatcher matcher, Instant now) {
        assertPending(now);
        this.confirmAttemptCount += 1;
        this.updatedAt = now;
        if (!matcher.matches(rawCode, codeHash)) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_CODE_INVALID,
                "Invalid verification code"
            );
        }
        this.verifiedAt = now;
        this.updatedAt = now;
    }

    public void consume(Instant now) {
        if (consumedAt != null) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_ALREADY_USED,
                "Email verification already used"
            );
        }
        if (verifiedAt == null || expiresAt.isBefore(now)) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_REQUIRED,
                "Email verification required before sign up"
            );
        }
        this.consumedAt = now;
        this.updatedAt = now;
    }

    public boolean isVerifiedAvailable(Instant now) {
        return verifiedAt != null && consumedAt == null && !expiresAt.isBefore(now);
    }

    private void assertReusable(Instant now) {
        if (consumedAt != null) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_ALREADY_USED,
                "Email verification already used"
            );
        }
        if (verifiedAt != null && !expiresAt.isBefore(now)) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_ALREADY_VERIFIED,
                "Email already verified"
            );
        }
    }

    private void assertPending(Instant now) {
        if (consumedAt != null) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_ALREADY_USED,
                "Email verification already used"
            );
        }
        if (verifiedAt != null && !expiresAt.isBefore(now)) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_ALREADY_VERIFIED,
                "Email already verified"
            );
        }
        if (expiresAt.isBefore(now)) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_EXPIRED,
                "Verification code expired"
            );
        }
    }

    public String verificationId() { return verificationId; }
    public String email() { return email; }
    public String codeHash() { return codeHash; }
    public Instant expiresAt() { return expiresAt; }
    public Instant verifiedAt() { return verifiedAt; }
    public Instant consumedAt() { return consumedAt; }
    public int resendCount() { return resendCount; }
    public int confirmAttemptCount() { return confirmAttemptCount; }
    public Instant lastSentAt() { return lastSentAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    @FunctionalInterface
    public interface CodeMatcher {
        boolean matches(String rawCode, String codeHash);
    }
}
