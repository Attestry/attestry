package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.SignUpEmailVerificationJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignUpEmailVerificationJpaRepository extends JpaRepository<SignUpEmailVerificationJpaEntity, String> {

    Optional<SignUpEmailVerificationJpaEntity> findByEmail(String email);
}
