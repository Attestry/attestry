package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.userauth.domain.auth.model.SignUpEmailVerification;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.SignUpEmailVerificationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SignUpEmailVerificationMapper implements DomainMapper<SignUpEmailVerification, SignUpEmailVerificationJpaEntity> {
    @Override
    public SignUpEmailVerification toDomain(SignUpEmailVerificationJpaEntity entity) {
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

    @Override
    public SignUpEmailVerificationJpaEntity toEntity(SignUpEmailVerification domain) {
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
}
