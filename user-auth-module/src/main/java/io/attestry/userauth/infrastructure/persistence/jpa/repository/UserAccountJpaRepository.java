package io.attestry.userauth.infrastructure.persistence.jpa.repository;

import io.attestry.userauth.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountJpaEntity, String> {
    Optional<UserAccountJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
