package io.attestry.userauth.domain.identity.repository;

import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findByEmail(Email email);

    Optional<UserAccount> findById(String userId);

    UserAccount save(UserAccount userAccount);
}
