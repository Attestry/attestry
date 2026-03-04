package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import java.util.Optional;

public interface UserAccountRepositoryPort {
    Optional<UserAccount> findByEmail(Email email);

    Optional<UserAccount> findByUserId(String userId);

    UserAccount saveNew(UserAccount userAccount);

    UserAccount save(UserAccount userAccount);
}
