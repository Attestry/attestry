package io.attestry.userauth.infrastructure.persistence.jpa.auth;

import io.attestry.userauth.application.port.auth.SignUpEmailVerificationRepositoryPort;
import io.attestry.userauth.domain.identity.model.SignUpEmailVerification;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.SignUpEmailVerificationJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.SignUpEmailVerificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaSignUpEmailVerificationRepositoryAdapter implements SignUpEmailVerificationRepositoryPort {

    private final SignUpEmailVerificationJpaRepository repository;

    @Override
    public Optional<SignUpEmailVerification> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public SignUpEmailVerification save(SignUpEmailVerification verification) {
        return toDomain(repository.save(toEntity(verification)));
    }

    private SignUpEmailVerificationJpaEntity toEntity(SignUpEmailVerification domain) {
        return new SignUpEmailVerificationJpaEntity(
            domain.verificationId(),
            domain.email(),
            domain.codeHash(),
            domain.expiresAt(),
            domain.verifiedAt(),
            domain.consumedAt(),
            domain.resendCount(),
            domain.confirmAttemptCount(),
            domain.lastSentAt(),
            domain.createdAt(),
            domain.updatedAt()
        );
    }

    private SignUpEmailVerification toDomain(SignUpEmailVerificationJpaEntity entity) {
        return SignUpEmailVerification.reconstitute(
            entity.getVerificationId(),
            entity.getEmail(),
            entity.getCodeHash(),
            entity.getExpiresAt(),
            entity.getVerifiedAt(),
            entity.getConsumedAt(),
            entity.getResendCount(),
            entity.getConfirmAttemptCount(),
            entity.getLastSentAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
