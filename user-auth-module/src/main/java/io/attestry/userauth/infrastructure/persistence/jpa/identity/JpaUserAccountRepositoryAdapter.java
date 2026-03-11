package io.attestry.userauth.infrastructure.persistence.jpa.identity;

import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.UserAccountMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.UserAccountJpaRepository;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaUserAccountRepositoryAdapter implements UserAccountRepositoryPort {

    private final UserAccountJpaRepository repository;
    private final UserAccountMapper userAccountMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email).map(userAccountMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(String userId) {
        return repository.findById(userId).map(userAccountMapper::toDomain);
    }

    @Override
    @Transactional
    public UserAccount save(UserAccount userAccount) {
        return userAccountMapper.toDomain(repository.save(userAccountMapper.toEntity(userAccount)));
    }
}
