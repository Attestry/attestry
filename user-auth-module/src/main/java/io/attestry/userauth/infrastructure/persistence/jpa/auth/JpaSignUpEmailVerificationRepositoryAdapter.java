package io.attestry.userauth.infrastructure.persistence.jpa.auth;

import io.attestry.userauth.application.port.auth.SignUpEmailVerificationRepositoryPort;
import io.attestry.userauth.domain.auth.model.SignUpEmailVerification;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.SignUpEmailVerificationMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.SignUpEmailVerificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaSignUpEmailVerificationRepositoryAdapter extends SignUpEmailVerificationMapper implements SignUpEmailVerificationRepositoryPort {

    private final SignUpEmailVerificationJpaRepository repository;

    @Override
    public Optional<SignUpEmailVerification> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public SignUpEmailVerification save(SignUpEmailVerification verification) {
        return toDomain(repository.save(toEntity(verification)));
    }
}
