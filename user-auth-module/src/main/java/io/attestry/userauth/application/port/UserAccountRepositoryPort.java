package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.user.vo.Email;
import io.attestry.userauth.domain.user.model.UserAccount;
import java.util.Optional;

public interface UserAccountRepositoryPort {
    Optional<UserAccount> findByEmail(Email email);

    Optional<UserAccount> findByUserId(String userId);

    UserAccount saveNew(UserAccount userAccount);

    UserAccount save(UserAccount userAccount);
}
