package io.attestry.userauth.application.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.identity.model.UserStatus;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserAccountQueryServiceTest {

    @Test
    void getEmailsByUserIds_returnsDistinctEmailMap() {
        UserAccount first = UserAccount.reconstitute(
            "user-1",
            Email.of("first@example.com"),
            null,
            "hash",
            UserStatus.ACTIVE,
            VerificationLevel.NONE
        );
        UserAccount second = UserAccount.reconstitute(
            "user-2",
            Email.of("second@example.com"),
            null,
            "hash",
            UserStatus.ACTIVE,
            VerificationLevel.PHONE_VERIFIED
        );
        UserAccountQueryService userAccountQueryService = new UserAccountQueryService(
            new StubUserAccountRepositoryPort(List.of(first, second))
        );

        Map<String, String> result = userAccountQueryService.getEmailsByUserIds(List.of("user-1", "user-2", "user-1"));

        assertEquals(2, result.size());
        assertEquals("first@example.com", result.get("user-1"));
        assertEquals("second@example.com", result.get("user-2"));
    }

    @Test
    void getEmailsByUserIds_returnsEmptyMap_whenIdsBlankOrNull() {
        UserAccountQueryService userAccountQueryService = new UserAccountQueryService(
            new StubUserAccountRepositoryPort(List.of())
        );

        assertTrue(userAccountQueryService.getEmailsByUserIds(List.of()).isEmpty());
        assertTrue(userAccountQueryService.getEmailsByUserIds(Arrays.asList((String) null)).isEmpty());
    }

    private static final class StubUserAccountRepositoryPort implements UserAccountRepositoryPort {

        private final List<UserAccount> users;

        private StubUserAccountRepositoryPort(List<UserAccount> users) {
            this.users = users;
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return users.stream().filter(user -> user.email().value().equals(email)).findFirst();
        }

        @Override
        public Optional<UserAccount> findById(String userId) {
            return users.stream().filter(user -> user.userId().equals(userId)).findFirst();
        }

        @Override
        public List<UserAccount> findByIds(List<String> userIds) {
            return users.stream().filter(user -> userIds.contains(user.userId())).toList();
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            throw new UnsupportedOperationException();
        }
    }
}
