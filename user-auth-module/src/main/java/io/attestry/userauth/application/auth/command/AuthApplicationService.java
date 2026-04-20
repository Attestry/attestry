package io.attestry.userauth.application.auth.command;

import io.attestry.userauth.application.auth.internal.AuthTokenIssuer;
import io.attestry.userauth.application.auth.internal.LoginContextResolver;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.auth.model.UserAccount;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthApplicationService implements AuthUseCase {

    private final UserAccountRepositoryPort userAccountRepository;
    private final LoginContextResolver loginContextResolver;
    private final PasswordHasherPort passwordHasher;
    private final AuthTokenIssuer authTokenIssuer;
    private final AccessTokenPort accessTokenPort;
    private final MembershipPort membershipPort;

    @Override
    public AuthTokenResult login(LoginCommand command) {
        UserAccount account = userAccountRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        account.assertPasswordMatches(command.password(), passwordHasher::matches);
        account.checkActiveStatus();

        LoginContext loginContext = loginContextResolver.resolve(account.userId(), command.tenantId());
        return authTokenIssuer.issue(account, loginContext);
    }

    @Override
    public void logout(String accessToken) {
        accessTokenPort.revoke(accessToken);
    }

    @Override
    public AuthPrincipal authenticate(String accessToken) {
        return accessTokenPort.parse(accessToken)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.ACCESS_TOKEN_INVALID, "Invalid access token"));
    }

    @Override
    public VerifyPhoneResult verifyPhone(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));
        account.verifyPhone();
        userAccountRepository.save(account);
        return new VerifyPhoneResult(account.userId(), account.verificationLevel());
    }

    // TODO: Token reissue needed because token info changes
    @Override
    public AuthTokenResult reissueToken(String userId, String tenantId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        account.checkActiveStatus();

        LoginContext loginContext = loginContextResolver.resolve(account.userId(), tenantId);
        return authTokenIssuer.issue(account, loginContext);
    }

    @Override
    public AuthTokenResult switchTenant(String userId, String membershipId) {
        UserAccount account = userAccountRepository.findById(userId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        account.checkActiveStatus();

        Membership membership = membershipPort.findMembershipByMembershipIdAndUserId(membershipId, userId)
            .filter(Membership::isActive)
            .orElseThrow(() -> new UserAuthDomainException(
                UserAuthErrorCode.MEMBERSHIP_NOT_FOUND,
                "Membership not found"
            ));

        LoginContext loginContext = loginContextResolver.resolve(account.userId(), membership.tenantId());
        return authTokenIssuer.issue(account, loginContext);
    }

    @Override
    public void resetPassword(String userId, ResetPasswordCommand command) {
        UserAccount account = userAccountRepository.findById(userId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        account.checkActiveStatus();
        account.changePassword(
            command.currentPassword(),
            passwordHasher.hash(command.newPassword()),
            passwordHasher::matches
        );
        userAccountRepository.save(account);
    }
}
