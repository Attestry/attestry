package io.attestry.userauth.application.port.identity;

import io.attestry.userauth.domain.auth.model.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepositoryPort {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(String userId);

    List<UserAccount> findByIds(List<String> userIds);

    UserAccount save(UserAccount userAccount);
}
