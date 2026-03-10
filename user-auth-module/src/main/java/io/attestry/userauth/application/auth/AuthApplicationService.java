package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.result.SignUpResult;
import io.attestry.userauth.application.dto.result.VerifyPhoneResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.application.port.AccessTokenPort;
import io.attestry.userauth.application.port.PasswordHasherPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.application.usecase.auth.AuthUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.UserAccount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import io.attestry.userauth.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthApplicationService implements AuthUseCase {

    private final UserAccountRepositoryPort userAccountRepository;
    private final LoginContextResolver loginContextResolver;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenPort accessTokenPort;
    private final Clock clock;
    @Value("${app.auth.token.access-ttl:PT15M}")
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    @Override
    public SignUpResult signUp(SignUpCommand command) {
        userAccountRepository.findByEmail(Email.of(command.email()).value())
                .ifPresent(account -> {
                    throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_EMAIL, "Email already exists");
                });

        String passwordHash = passwordHasher.hash(command.password());
        UserAccount userAccount = UserAccount.register(command.email(), command.phone(), passwordHash);
        return new SignUpResult(userAccountRepository.save(userAccount).userId());
    }

    @Override
    public AuthTokenResult login(LoginCommand command) {
        UserAccount account = userAccountRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        account.assertPasswordMatches(command.password(), passwordHasher::matches);
        account.checkActiveStatus();

        LoginContext loginContext = loginContextResolver.resolve(account.userId(), command.tenantId());

        Instant now = Instant.now(clock);
        AuthPrincipal principal = AuthPrincipal.issue(
                account.userId(),
                loginContext.tenantId(),
                account.verificationLevel(),
                loginContext.scopes(),
                now,
                accessTokenTtl);
        String token = accessTokenPort.issue(principal);

        return new AuthTokenResult(
                token,
                "Bearer",
                principal.expiresAt(),
                account.userId(),
                loginContext.tenantId());
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

    @Override
    public AuthTokenResult reissueToken(String userId, String tenantId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        account.checkActiveStatus();

        LoginContext loginContext = loginContextResolver.resolve(account.userId(), tenantId);

        Instant now = Instant.now(clock);
        AuthPrincipal principal = AuthPrincipal.issue(
                account.userId(),
                loginContext.tenantId(),
                account.verificationLevel(),
                loginContext.scopes(),
                now,
                accessTokenTtl);
        String token = accessTokenPort.issue(principal);

        return new AuthTokenResult(
                token,
                "Bearer",
                principal.expiresAt(),
                account.userId(),
                loginContext.tenantId());
    }
}
