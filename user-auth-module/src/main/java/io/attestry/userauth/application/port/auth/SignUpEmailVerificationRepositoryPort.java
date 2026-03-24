package io.attestry.userauth.application.port.auth;

import io.attestry.userauth.domain.auth.model.SignUpEmailVerification;
import java.util.Optional;

public interface SignUpEmailVerificationRepositoryPort {

    Optional<SignUpEmailVerification> findByEmail(String email);

    SignUpEmailVerification save(SignUpEmailVerification verification);
}
