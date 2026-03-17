package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "signup_email_verifications")
public class SignUpEmailVerificationJpaEntity {

    @Id
    @Column(name = "verification_id", nullable = false, length = 36)
    private String verificationId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(name = "confirm_attempt_count", nullable = false)
    private int confirmAttemptCount;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SignUpEmailVerificationJpaEntity() {
    }

    public SignUpEmailVerificationJpaEntity(
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

    public String getVerificationId() { return verificationId; }
    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public int getResendCount() { return resendCount; }
    public int getConfirmAttemptCount() { return confirmAttemptCount; }
    public Instant getLastSentAt() { return lastSentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
