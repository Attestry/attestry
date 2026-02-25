package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.user.enums.UserStatus;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
public class UserAccountJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_level", nullable = false)
    private VerificationLevel verificationLevel;

    protected UserAccountJpaEntity() {
    }

    public UserAccountJpaEntity(
        String userId,
        String email,
        String passwordHash,
        String phone,
        UserStatus status,
        VerificationLevel verificationLevel
    ) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.status = status;
        this.verificationLevel = verificationLevel;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public VerificationLevel getVerificationLevel() {
        return verificationLevel;
    }

    public void setVerificationLevel(VerificationLevel verificationLevel) {
        this.verificationLevel = verificationLevel;
    }
}
