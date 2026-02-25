package io.attestry.userauth.infrastructure.persistence.jpa;

import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.user.vo.Email;
import io.attestry.userauth.domain.user.model.User;
import io.attestry.userauth.domain.user.model.UserAccount;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaUserAccountRepositoryAdapter implements UserAccountRepositoryPort {

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
    @Transactional
    public UserAccount saveNew(UserAccount userAccount) {
        String email = userAccount.user().email().value();
        if (repository.existsByEmail(email)) {
            throw new DomainException(ErrorCode.DUPLICATE_EMAIL, "Email already exists");
        }

        UserAccountJpaEntity saved = repository.save(new UserAccountJpaEntity(
            userAccount.user().userId(),
            email,
            userAccount.passwordHash(),
            userAccount.user().phone(),
            userAccount.user().status(),
            userAccount.user().verificationLevel()
        ));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void updateVerificationLevel(String userId, VerificationLevel verificationLevel) {
        repository.findById(userId).ifPresent(entity -> {
            entity.setVerificationLevel(verificationLevel);
            repository.save(entity);
        });
    }

    private UserAccount toDomain(UserAccountJpaEntity entity) {
        return new UserAccount(
            new User(
                entity.getUserId(),
                Email.of(entity.getEmail()),
                entity.getPhone(),
                entity.getStatus(),
                entity.getVerificationLevel()
            ),
            entity.getPasswordHash()
        );
    }
}
