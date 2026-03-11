package io.attestry.userauth.application.port.identity;

import io.attestry.userauth.domain.identity.model.UserAccount;
import java.util.Optional;

public interface UserAccountRepositoryPort {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(String userId);

    UserAccount save(UserAccount userAccount);
}
