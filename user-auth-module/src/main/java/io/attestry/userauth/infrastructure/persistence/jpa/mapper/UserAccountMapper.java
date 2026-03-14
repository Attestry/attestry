package io.attestry.userauth.infrastructure.persistence.jpa.mapper;

import io.attestry.commonlib.infrastructure.DomainMapper;
import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.UserAccountJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserAccountMapper implements DomainMapper<UserAccount, UserAccountJpaEntity> {
    @Override
    public UserAccount toDomain(UserAccountJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserAccount.reconstitute(
            entity.getUserId(),
            Email.of(entity.getEmail()),
            entity.getPhone(),
            entity.getPasswordHash(),
            entity.getStatus(),
            entity.getVerificationLevel()
        );
    }

    @Override
    public UserAccountJpaEntity toEntity(UserAccount domain) {
        if (domain == null) {
            return null;
        }
        return new UserAccountJpaEntity(
            domain.userId(),
            domain.email().value(),
            domain.passwordHash(),
            domain.phone(),
            domain.status(),
            domain.verificationLevel()
        );
    }
}
