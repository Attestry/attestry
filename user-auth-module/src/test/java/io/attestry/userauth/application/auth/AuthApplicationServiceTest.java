package io.attestry.userauth.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.application.port.AccessTokenPort;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.port.PasswordHasherPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.organization.model.GroupStatus;
import io.attestry.userauth.domain.organization.model.GroupType;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.organization.model.TenantStatus;
import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.identity.model.UserStatus;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthApplicationServiceTest {

    private InMemoryUserAccountRepo userRepo;
    private InMemoryMembershipRepo membershipRepo;
    private FakePasswordHasher passwordHasher;
    private InMemoryAccessTokenPort tokenPort;
    private InMemoryMembershipPermissionQueryPort permissionQueryPort;
    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        userRepo = new InMemoryUserAccountRepo();
        membershipRepo = new InMemoryMembershipRepo();
        passwordHasher = new FakePasswordHasher();
        tokenPort = new InMemoryAccessTokenPort();
        permissionQueryPort = new InMemoryMembershipPermissionQueryPort();
        permissionQueryPort.seedGlobalRole(RoleCodes.OWNER_DEFAULT, Set.of(
            PermissionCodes.OWNER_TRANSFER_CREATE,
            PermissionCodes.OWNER_TRANSFER_ACCEPT,
            PermissionCodes.OWNER_RISK_FLAG,
            PermissionCodes.OWNER_RISK_CLEAR
        ));
        Clock clock = Clock.fixed(Instant.parse("2026-02-25T00:00:00Z"), ZoneOffset.UTC);

        service = new AuthApplicationService(
            userRepo,
            membershipRepo,
            permissionQueryPort,
            passwordHasher,
            tokenPort,
            clock,
            Duration.ofMinutes(15)
        );
    }

    @Test
    void signUpShouldHashPasswordAndReturnUserId() {
        String userId = service.signUp(new SignUpCommand("a@b.com", "plain", "010-0000")).userId();

        UserAccount saved = userRepo.findByUserId(userId).orElseThrow();
        assertEquals("hashed:plain", saved.passwordHash());
        assertEquals("a@b.com", saved.email().value());
    }

    @Test
    void loginShouldIssueTokenWithMembershipContextAndScopes() {
        userRepo.seed("u1", "admin@brand.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        membershipRepo.seed(Membership.reconstitute(
            "m1", "u1", "g1", "t1",
            GroupType.BRAND, MembershipRole.ADMIN, MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE, TenantStatus.ACTIVE, Set.of()
        ));
        permissionQueryPort.seed("m1", Set.of(
            PermissionCodes.TENANT_MEMBERSHIP_VIEW,
            PermissionCodes.BRAND_MINT
        ));

        AuthTokenResult result = service.login(new LoginCommand("admin@brand.com", "pw", "t1", "g1"));
        AuthPrincipal principal = tokenPort.parse(result.accessToken()).orElseThrow();

        assertEquals("t1", result.tenantId());
        assertEquals("g1", result.groupId());
        assertTrue(principal.scopes().contains(PermissionCodes.OWNER_TRANSFER_CREATE));
        assertTrue(principal.scopes().contains(PermissionCodes.TENANT_MEMBERSHIP_VIEW));
        assertTrue(principal.scopes().contains(PermissionCodes.BRAND_MINT));
    }

    @Test
    void loginWithoutMembershipShouldStillIssueOwnerScopes() {
        userRepo.seed("u2", "owner@x.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);

        AuthTokenResult result = service.login(new LoginCommand("owner@x.com", "pw", null, null));
        AuthPrincipal principal = tokenPort.parse(result.accessToken()).orElseThrow();

        assertEquals(null, result.tenantId());
        assertEquals(null, result.groupId());
        assertEquals(Set.of(
            PermissionCodes.OWNER_TRANSFER_CREATE,
            PermissionCodes.OWNER_TRANSFER_ACCEPT,
            PermissionCodes.OWNER_RISK_FLAG,
            PermissionCodes.OWNER_RISK_CLEAR
        ), principal.scopes());
    }

    @Test
    void loginShouldFailWhenUserNotFound() {
        DomainException ex = assertThrows(DomainException.class,
            () -> service.login(new LoginCommand("missing@x.com", "pw", null, null)));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenPasswordInvalid() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);

        DomainException ex = assertThrows(DomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "wrong", null, null)));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenUserSuspended() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.SUSPENDED, VerificationLevel.NONE);

        DomainException ex = assertThrows(DomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "pw", null, null)));

        assertEquals(ErrorCode.USER_SUSPENDED, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenRequestedMembershipNotFound() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        membershipRepo.seed(Membership.reconstitute(
            "m1", "u1", "g1", "t1",
            GroupType.RETAIL, MembershipRole.OPERATOR, MembershipStatus.ACTIVE,
            GroupStatus.ACTIVE, TenantStatus.ACTIVE, Set.of()
        ));

        DomainException ex = assertThrows(DomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "pw", "t2", "g2")));

        assertEquals(ErrorCode.MEMBERSHIP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenRequestedMembershipInactive() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        membershipRepo.seed(Membership.reconstitute(
            "m1", "u1", "g1", "t1",
            GroupType.RETAIL, MembershipRole.OPERATOR, MembershipStatus.SUSPENDED,
            GroupStatus.ACTIVE, TenantStatus.ACTIVE, Set.of()
        ));

        DomainException ex = assertThrows(DomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "pw", "t1", "g1")));

        assertEquals(ErrorCode.MEMBERSHIP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void logoutShouldRevokeToken() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        AuthTokenResult result = service.login(new LoginCommand("a@b.com", "pw", null, null));

        service.logout(result.accessToken());

        assertTrue(tokenPort.parse(result.accessToken()).isEmpty());
    }

    @Test
    void authenticateShouldReturnPrincipal() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        AuthTokenResult result = service.login(new LoginCommand("a@b.com", "pw", null, null));

        AuthPrincipal principal = service.authenticate(result.accessToken());

        assertNotNull(principal);
        assertEquals("u1", principal.userId());
    }

    @Test
    void authenticateShouldFailForInvalidToken() {
        DomainException ex = assertThrows(DomainException.class, () -> service.authenticate("bad-token"));

        assertEquals(ErrorCode.ACCESS_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void verifyPhoneShouldUpdateVerificationLevel() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);

        service.verifyPhone("u1");

        VerificationLevel updated = userRepo.findByUserId("u1").orElseThrow().verificationLevel();
        assertEquals(VerificationLevel.PHONE_VERIFIED, updated);
    }

    @Test
    void verifyPhoneShouldFailWhenUserNotFound() {
        DomainException ex = assertThrows(DomainException.class, () -> service.verifyPhone("missing"));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    private static class InMemoryUserAccountRepo implements UserAccountRepositoryPort {

        private final Map<String, UserAccount> byUserId = new HashMap<>();
        private final Map<String, String> userIdByEmail = new HashMap<>();

        @Override
        public Optional<UserAccount> findByEmail(Email email) {
            String userId = userIdByEmail.get(email.value());
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
            byUserId.put(userAccount.userId(), userAccount);
            userIdByEmail.put(userAccount.email().value(), userAccount.userId());
            return userAccount;
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            byUserId.put(userAccount.userId(), userAccount);
            userIdByEmail.put(userAccount.email().value(), userAccount.userId());
            return userAccount;
        }

        void seed(String userId, String email, String passwordHash, UserStatus status, VerificationLevel level) {
            UserAccount account = UserAccount.reconstitute(userId, Email.of(email), "010", passwordHash, status, level);
            byUserId.put(userId, account);
            userIdByEmail.put(email, userId);
        }
    }

    private static class InMemoryMembershipRepo implements MembershipRepositoryPort {

        private final List<Membership> memberships = new ArrayList<>();

        @Override
        public List<Membership> findByUserId(String userId) {
            return memberships.stream().filter(m -> m.userId().equals(userId)).toList();
        }

        @Override
        public Optional<Membership> findByUserIdAndContext(String userId, String tenantId, String groupId) {
            return memberships.stream()
                .filter(m -> m.userId().equals(userId))
                .filter(m -> m.tenantId().equals(tenantId))
                .filter(m -> m.groupId().equals(groupId))
                .findFirst();
        }

        void seed(Membership membership) {
            memberships.add(membership);
        }
    }

    private static class FakePasswordHasher implements PasswordHasherPort {

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hashedPassword) {
            return hash(rawPassword).equals(hashedPassword);
        }
    }

    private static class InMemoryAccessTokenPort implements AccessTokenPort {

        private final Map<String, AuthPrincipal> tokens = new HashMap<>();

        @Override
        public String issue(AuthPrincipal principal) {
            String token = UUID.randomUUID().toString();
            tokens.put(token, principal);
            return token;
        }

        @Override
        public Optional<AuthPrincipal> parse(String token) {
            return Optional.ofNullable(tokens.get(token));
        }

        @Override
        public void revoke(String token) {
            tokens.remove(token);
        }
    }

    private static class InMemoryMembershipPermissionQueryPort implements MembershipPermissionQueryPort {
        private final Map<String, Set<String>> permissionByMembershipId = new HashMap<>();
        private final Map<String, Set<String>> permissionByRoleCode = new HashMap<>();

        @Override
        public Set<String> findPermissionCodesByMembershipId(String membershipId) {
            return permissionByMembershipId.getOrDefault(membershipId, Set.of());
        }

        @Override
        public Set<String> findPermissionCodesByGlobalRoleCode(String roleCode) {
            return permissionByRoleCode.getOrDefault(roleCode, Set.of());
        }

        @Override
        public Set<String> findRoleCodesByMembershipId(String membershipId) {
            return Set.of();
        }

        void seed(String membershipId, Set<String> codes) {
            permissionByMembershipId.put(membershipId, codes);
        }

        void seedGlobalRole(String roleCode, Set<String> codes) {
            permissionByRoleCode.put(roleCode, codes);
        }
    }
}
