package io.attestry.userauth.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.application.port.AccessTokenPort;
import io.attestry.userauth.application.port.MembershipPort;
import io.attestry.userauth.application.port.MembershipProjectionPort;
import io.attestry.userauth.application.port.PasswordHasherPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.domain.authorization.model.PermissionCodes;
import io.attestry.userauth.domain.authorization.model.RoleCodes;
import io.attestry.userauth.domain.tenant.model.TenantType;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.tenant.model.TenantStatus;
import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.identity.model.UserStatus;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import java.time.Clock;
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
        LoginContextResolver loginContextResolver = new LoginContextResolver(
            membershipRepo,
            permissionQueryPort
        );

        service = new AuthApplicationService(
            userRepo,
            loginContextResolver,
            passwordHasher,
            tokenPort,
            clock
        );
    }

    @Test
    void signUpShouldHashPasswordAndReturnUserId() {
        String userId = service.signUp(new SignUpCommand("a@b.com", "plain", "010-0000")).userId();

        UserAccount saved = userRepo.findById(userId).orElseThrow();
        assertEquals("hashed:plain", saved.passwordHash());
        assertEquals("a@b.com", saved.email().value());
    }

    @Test
    void loginShouldIssueTokenWithMembershipContextAndScopes() {
        userRepo.seed("u1", "admin@brand.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        membershipRepo.seed(Membership.reconstitute(
            "m1", "u1", "t1",
            TenantType.BRAND, MembershipRole.ADMIN, MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE, Set.of()
        ));
        permissionQueryPort.seed("m1", Set.of(
            PermissionCodes.TENANT_MEMBERSHIP_VIEW,
            PermissionCodes.BRAND_MINT
        ));

        AuthTokenResult result = service.login(new LoginCommand("admin@brand.com", "pw", "t1"));
        AuthPrincipal principal = tokenPort.parse(result.accessToken()).orElseThrow();

        assertEquals("t1", result.tenantId());
        assertTrue(principal.scopes().contains(PermissionCodes.OWNER_TRANSFER_CREATE));
        assertTrue(principal.scopes().contains(PermissionCodes.TENANT_MEMBERSHIP_VIEW));
        assertTrue(principal.scopes().contains(PermissionCodes.BRAND_MINT));
    }

    @Test
    void loginWithoutMembershipShouldStillIssueOwnerScopes() {
        userRepo.seed("u2", "owner@x.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);

        AuthTokenResult result = service.login(new LoginCommand("owner@x.com", "pw", null));
        AuthPrincipal principal = tokenPort.parse(result.accessToken()).orElseThrow();

        assertEquals(null, result.tenantId());
        assertEquals(Set.of(
            PermissionCodes.OWNER_TRANSFER_CREATE,
            PermissionCodes.OWNER_TRANSFER_ACCEPT,
            PermissionCodes.OWNER_RISK_FLAG,
            PermissionCodes.OWNER_RISK_CLEAR
        ), principal.scopes());
    }

    @Test
    void loginShouldFailWhenUserNotFound() {
        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class,
            () -> service.login(new LoginCommand("missing@x.com", "pw", null)));

        assertEquals(UserAuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenPasswordInvalid() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);

        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "wrong", null)));

        assertEquals(UserAuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenUserSuspended() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.SUSPENDED, VerificationLevel.NONE);

        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "pw", null)));

        assertEquals(UserAuthErrorCode.USER_SUSPENDED, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenRequestedMembershipNotFound() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        membershipRepo.seed(Membership.reconstitute(
            "m1", "u1", "t1",
            TenantType.RETAIL, MembershipRole.OPERATOR, MembershipStatus.ACTIVE,
            TenantStatus.ACTIVE, Set.of()
        ));

        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "pw", "t2")));

        assertEquals(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void loginShouldFailWhenRequestedMembershipInactive() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        membershipRepo.seed(Membership.reconstitute(
            "m1", "u1", "t1",
            TenantType.RETAIL, MembershipRole.OPERATOR, MembershipStatus.SUSPENDED,
            TenantStatus.ACTIVE, Set.of()
        ));

        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class,
            () -> service.login(new LoginCommand("a@b.com", "pw", "t1")));

        assertEquals(UserAuthErrorCode.MEMBERSHIP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void logoutShouldRevokeToken() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        AuthTokenResult result = service.login(new LoginCommand("a@b.com", "pw", null));

        service.logout(result.accessToken());

        assertTrue(tokenPort.parse(result.accessToken()).isEmpty());
    }

    @Test
    void authenticateShouldReturnPrincipal() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);
        AuthTokenResult result = service.login(new LoginCommand("a@b.com", "pw", null));

        AuthPrincipal principal = service.authenticate(result.accessToken());

        assertNotNull(principal);
        assertEquals("u1", principal.userId());
    }

    @Test
    void authenticateShouldFailForInvalidToken() {
        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class, () -> service.authenticate("bad-token"));

        assertEquals(UserAuthErrorCode.ACCESS_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void verifyPhoneShouldUpdateVerificationLevel() {
        userRepo.seed("u1", "a@b.com", "hashed:pw", UserStatus.ACTIVE, VerificationLevel.NONE);

        service.verifyPhone("u1");

        VerificationLevel updated = userRepo.findById("u1").orElseThrow().verificationLevel();
        assertEquals(VerificationLevel.PHONE_VERIFIED, updated);
    }

    @Test
    void verifyPhoneShouldFailWhenUserNotFound() {
        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class, () -> service.verifyPhone("missing"));

        assertEquals(UserAuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    private static class InMemoryUserAccountRepo implements UserAccountRepositoryPort {

        private final Map<String, UserAccount> byUserId = new HashMap<>();
        private final Map<String, String> userIdByEmail = new HashMap<>();

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            String userId = userIdByEmail.get(email);
            if (userId == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(byUserId.get(userId));
        }

        @Override
        public Optional<UserAccount> findById(String userId) {
            return Optional.ofNullable(byUserId.get(userId));
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

    private static class InMemoryMembershipRepo implements MembershipPort {

        private final List<Membership> memberships = new ArrayList<>();

        @Override
        public Optional<Membership> findById(String membershipId) {
            return memberships.stream().filter(m -> m.membershipId().equals(membershipId)).findFirst();
        }

        @Override
        public Membership save(Membership membership) {
            memberships.add(membership);
            return membership;
        }

        @Override
        public List<Membership> findByUserId(String userId) {
            return memberships.stream().filter(m -> m.userId().equals(userId)).toList();
        }

        @Override
        public Optional<Membership> findByUserIdAndTenantId(String userId, String tenantId) {
            return memberships.stream()
                .filter(m -> m.userId().equals(userId))
                .filter(m -> m.tenantId().equals(tenantId))
                .findFirst();
        }

        @Override
        public List<Membership> findByTenantId(String tenantId) {
            return memberships.stream().filter(m -> m.tenantId().equals(tenantId)).toList();
        }

        @Override
        public List<Membership> findMembershipsByTenantId(String tenantId) {
            return findByTenantId(tenantId);
        }

        @Override
        public Optional<Membership> findMembershipById(String membershipId) {
            return findById(membershipId);
        }

        @Override
        public Membership updateMembership(String tenantId, String membershipId, MembershipRole role, MembershipStatus status) {
            return null;
        }

        @Override
        public void assignRole(String membershipId, String roleCode, String assignedByUserId) {}

        @Override
        public void deletePermissionOverrides(String membershipId, Set<String> permissionCodes) {}

        @Override
        public Set<String> applyPermissionTemplateToMembership(String membershipId, String templateCode, String reason, String actorUserId, Instant now) {
            return Set.of();
        }

        @Override
        public Set<String> revokePermissionTemplateFromMembership(String membershipId, String templateCode) {
            return Set.of();
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

    private static class InMemoryMembershipPermissionQueryPort implements MembershipProjectionPort {
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

        @Override
        public Set<String> findGlobalEnabledRoleCodes() {
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
