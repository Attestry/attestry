package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.result.SignUpResult;
import io.attestry.userauth.application.dto.result.VerifyPhoneResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.application.port.AccessTokenPort;
import io.attestry.userauth.application.port.MembershipPermissionQueryPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.port.PasswordHasherPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.application.usecase.auth.AuthUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.auth.model.LoginContext;
import io.attestry.userauth.domain.auth.model.RoleCodes;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.membership.policy.MembershipSelectionPolicy;
import io.attestry.userauth.domain.user.vo.Email;
import io.attestry.userauth.domain.user.model.UserAccount;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService implements AuthUseCase {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(24);

    private final UserAccountRepositoryPort userAccountRepository;
    private final MembershipRepositoryPort membershipRepository;
    private final MembershipPermissionQueryPort membershipPermissionQueryPort;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final Clock clock;

    public AuthApplicationService(
        UserAccountRepositoryPort userAccountRepository,
        MembershipRepositoryPort membershipRepository,
        MembershipPermissionQueryPort membershipPermissionQueryPort,
        PasswordHasherPort passwordHasher,
        AccessTokenPort accessTokenPort,
        Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.membershipPermissionQueryPort = membershipPermissionQueryPort;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.clock = clock;
    }

    @Override
    public SignUpResult signUp(SignUpCommand command) {
        String passwordHash = passwordHasher.hash(command.password());
        UserAccount userAccount = UserAccount.register(command.email(), command.phone(), passwordHash);
        return new SignUpResult(userAccountRepository.saveNew(userAccount).user().userId());
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {
        UserAccount account = userAccountRepository.findByEmail(Email.of(command.email()))
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));

        account.assertPasswordMatches(command.password(), passwordHasher::matches);
        account.checkActiveStatus();

        LoginContext loginContext = resolveLoginContext(account.userId(), command.tenantId(), command.groupId());

        Instant now = Instant.now(clock);
        AuthPrincipal principal = AuthPrincipal.issue(
            account.userId(),
            loginContext.tenantId(),
            loginContext.groupId(),
            account.verificationLevel(),
            loginContext.scopes(),
            now,
            ACCESS_TOKEN_TTL
        );
        String token = accessTokenPort.issue(principal);

        return new AuthTokenResult(
            token,
            "Bearer",
            principal.expiresAt(),
            account.userId(),
            loginContext.tenantId(),
            loginContext.groupId()
        );
    }

    @Override
    public void logout(String accessToken) {
        accessTokenPort.revoke(accessToken);
    }

    @Override
    public AuthPrincipal authenticate(String accessToken) {
        return accessTokenPort.parse(accessToken)
            .orElseThrow(() -> new DomainException(ErrorCode.ACCESS_TOKEN_INVALID, "Invalid access token"));
    }

    private Membership resolveActiveMembership(String userId, String tenantId, String groupId) {
        Optional<Membership> requestedMembership = (tenantId != null && groupId != null)
            ? membershipRepository.findByUserIdAndContext(userId, tenantId, groupId)
            : Optional.empty();
        List<Membership> memberships = membershipRepository.findByUserId(userId);
        return MembershipSelectionPolicy.resolve(tenantId, groupId, requestedMembership, memberships);
    }

    private LoginContext resolveLoginContext(String userId, String tenantId, String groupId) {
        Membership activeMembership = resolveActiveMembership(userId, tenantId, groupId);
        Set<String> scopes = resolveOwnerPermissions();

        if (activeMembership == null) {
            return LoginContext.owner(scopes);
        }

        scopes.addAll(resolveMembershipScopes(activeMembership));
        return LoginContext.withMembership(activeMembership, scopes);
    }

    private Set<String> resolveMembershipScopes(Membership membership) {
        Set<String> permissionCodes = membershipPermissionQueryPort.findPermissionCodesByMembershipId(membership.membershipId());
        return Set.copyOf(permissionCodes);
    }

    private Set<String> resolveOwnerPermissions() {
        Set<String> permissionCodes = membershipPermissionQueryPort.findPermissionCodesByGlobalRoleCode(RoleCodes.OWNER_DEFAULT);
        if (permissionCodes.isEmpty()) {
            throw new IllegalStateException("OWNER_DEFAULT role permissions are not configured");
        }
        return new HashSet<>(permissionCodes);
    }

    // TODO("핸드폰 문자 발송")
    @Override
    public VerifyPhoneResult verifyPhone(String userId) {
        UserAccount account = userAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));
        UserAccount verified = userAccountRepository.save(account.verifyPhone());
        return new VerifyPhoneResult(verified.userId(), verified.verificationLevel());
    }
}
