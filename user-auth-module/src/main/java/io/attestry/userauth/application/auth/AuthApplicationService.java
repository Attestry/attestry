package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.dto.AuthTokenResult;
import io.attestry.userauth.application.dto.LoginCommand;
import io.attestry.userauth.application.dto.SignUpCommand;
import io.attestry.userauth.application.port.AccessTokenPort;
import io.attestry.userauth.application.port.MembershipRepositoryPort;
import io.attestry.userauth.application.port.PasswordHasherPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.auth.model.Scope;
import io.attestry.userauth.domain.user.vo.Email;
import io.attestry.userauth.domain.user.model.UserAccount;
import io.attestry.userauth.domain.user.model.User;
import io.attestry.userauth.domain.user.enums.UserStatus;
import io.attestry.userauth.domain.user.enums.VerificationLevel;
import io.attestry.userauth.domain.policy.ScopePolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

// TODO("interface도 외부라고 보면 port/in으로 들어오는것이 아닌가? 바로 서비스 호출?")
@Service
public class AuthApplicationService {

    private static final long ACCESS_TOKEN_MINUTES = 30;

    private final UserAccountRepositoryPort userAccountRepository;
    private final MembershipRepositoryPort membershipRepository;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final Clock clock;

    public AuthApplicationService(
        UserAccountRepositoryPort userAccountRepository,
        MembershipRepositoryPort membershipRepository,
        PasswordHasherPort passwordHasher,
        AccessTokenPort accessTokenPort,
        Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenPort = accessTokenPort;
        this.clock = clock;
    }

    public String signUp(SignUpCommand command) {
        String passwordHash = passwordHasher.hash(command.password());
        UserAccount userAccount = UserAccount.register(command.email(), command.phone(), passwordHash);
        return userAccountRepository.saveNew(userAccount).user().userId();
    }

    public AuthTokenResult login(LoginCommand command) {
        UserAccount account = userAccountRepository.findByEmail(Email.of(command.email()))
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (!passwordHasher.matches(command.password(), account.passwordHash())) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }

        User user = account.user();
        // TODO("이건 ")
        if (user.status() != UserStatus.ACTIVE) {
            throw new DomainException(ErrorCode.USER_SUSPENDED, "User is suspended");
        }

        Membership activeMembership = resolveActiveMembership(user.userId(), command.tenantId(), command.groupId());

        Set<Scope> scopes = ScopePolicy.ownerDefaultScopes();
        String tenantId = null;
        String groupId = null;

        if (activeMembership != null) {
            scopes.addAll(ScopePolicy.forMembership(activeMembership));
            tenantId = activeMembership.tenantId();
            groupId = activeMembership.groupId();
        }

        Instant expiresAt = Instant.now(clock).plus(ACCESS_TOKEN_MINUTES, ChronoUnit.MINUTES);
        AuthPrincipal principal = new AuthPrincipal(
            UUID.randomUUID().toString(),
            user.userId(),
            tenantId,
            groupId,
            user.verificationLevel(),
            scopes,
            expiresAt
        );
        String token = accessTokenPort.issue(principal);

        return new AuthTokenResult(token, "Bearer", expiresAt, user.userId(), tenantId, groupId);
    }

    public void logout(String accessToken) {
        accessTokenPort.revoke(accessToken);
    }

    public AuthPrincipal authenticate(String accessToken) {
        return accessTokenPort.parse(accessToken)
            .orElseThrow(() -> new DomainException(ErrorCode.ACCESS_TOKEN_INVALID, "Invalid access token"));
    }

    private Membership resolveActiveMembership(String userId, String tenantId, String groupId) {
        if (tenantId != null && groupId != null) {
            Membership membership = membershipRepository.findByUserIdAndContext(userId, tenantId, groupId)
                .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));
            if (!membership.isActive()) {
                throw new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership inactive");
            }
            return membership;
        }

        List<Membership> memberships = membershipRepository.findByUserId(userId);
        return memberships.stream().filter(Membership::isActive).findFirst().orElse(null);
    }

    public void verifyPhone(String userId) {
        userAccountRepository.findByUserId(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));
        userAccountRepository.updateVerificationLevel(userId, VerificationLevel.PHONE_VERIFIED);
    }
}
