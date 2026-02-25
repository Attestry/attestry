package io.attestry.userauth.infrastructure.persistence.inmemory;

import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.user.vo.Email;
import io.attestry.userauth.domain.user.model.User;
import io.attestry.userauth.domain.user.model.UserAccount;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("inmemory")
public class InMemoryUserAccountRepository implements UserAccountRepositoryPort {

    private final Map<String, UserAccount> byUserId = new ConcurrentHashMap<>();
    private final Map<String, String> emailToUserId = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findByEmail(Email email) {
        String userId = emailToUserId.get(email.value());
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byUserId.get(userId));
    }

    @Override
    public Optional<UserAccount> findByUserId(String userId) {
        return Optional.ofNullable(byUserId.get(userId));
    }

    @Override
    public UserAccount saveNew(UserAccount userAccount) {
        String email = userAccount.user().email().value();
        if (emailToUserId.containsKey(email)) {
            throw new DomainException(ErrorCode.DUPLICATE_EMAIL, "Email already exists");
        }

        UserAccount normalized = new UserAccount(
            new User(
                userAccount.user().userId(),
                userAccount.user().email(),
                userAccount.user().phone(),
                userAccount.user().status(),
                userAccount.user().verificationLevel()
            ),
            userAccount.passwordHash()
        );
        byUserId.put(normalized.user().userId(), normalized);
        emailToUserId.put(email, normalized.user().userId());
        return normalized;
    }

    @Override
    public void updateVerificationLevel(String userId, VerificationLevel verificationLevel) {
        UserAccount current = byUserId.get(userId);
        if (current == null) {
            return;
        }
        UserAccount updated = current.withVerificationLevel(verificationLevel);
        byUserId.put(userId, updated);
    }
}
