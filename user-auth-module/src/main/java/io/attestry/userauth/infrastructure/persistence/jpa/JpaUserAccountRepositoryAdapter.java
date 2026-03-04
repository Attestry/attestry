package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.identity.repository.UserAccountRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaUserAccountRepositoryAdapter implements UserAccountRepositoryPort, UserAccountRepository {

    private final UserAccountJpaRepository repository;

    public JpaUserAccountRepositoryAdapter(UserAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByUserId(String userId) {
        return repository.findById(userId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(String userId) {
        return findByUserId(userId);
    }

    @Override
    @Transactional
    public UserAccount saveNew(UserAccount userAccount) {
        String email = userAccount.email().value();
        if (repository.existsByEmail(email)) {
            throw new DomainException(ErrorCode.DUPLICATE_EMAIL, "Email already exists");
        }

        UserAccountJpaEntity saved = repository.save(new UserAccountJpaEntity(
            userAccount.userId(),
            email,
            userAccount.passwordHash(),
            userAccount.phone(),
            userAccount.status(),
            userAccount.verificationLevel()
        ));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount userAccount) {
        UserAccountJpaEntity saved = repository.save(new UserAccountJpaEntity(
            userAccount.userId(),
            userAccount.email().value(),
            userAccount.passwordHash(),
            userAccount.phone(),
            userAccount.status(),
            userAccount.verificationLevel()
        ));
        return toDomain(saved);
    }

    private UserAccount toDomain(UserAccountJpaEntity entity) {
        return UserAccount.reconstitute(
            entity.getUserId(),
            Email.of(entity.getEmail()),
            entity.getPhone(),
            entity.getPasswordHash(),
            entity.getStatus(),
            entity.getVerificationLevel()
        );
    }
}
