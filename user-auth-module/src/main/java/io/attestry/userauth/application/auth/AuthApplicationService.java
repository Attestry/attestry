package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.result.SignUpResult;
import io.attestry.userauth.application.dto.result.SignUpEmailVerificationResult;
import io.attestry.userauth.application.dto.result.VerifyPhoneResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.application.port.auth.AccessTokenPort;
import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import io.attestry.userauth.application.port.auth.SignUpEmailVerificationRepositoryPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.application.port.membership.MembershipPort;
import io.attestry.userauth.application.port.notification.NotificationOutboxRepositoryPort;
import io.attestry.userauth.application.usecase.auth.AuthUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.authorization.model.LoginContext;
import io.attestry.userauth.domain.identity.model.Email;
import io.attestry.userauth.domain.identity.model.SignUpEmailVerification;
import io.attestry.userauth.domain.identity.model.SignUpEmailVerificationNotificationPayload;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.security.AuthPrincipal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService implements AuthUseCase {

    private static final Duration SIGNUP_EMAIL_VERIFICATION_TTL = Duration.ofMinutes(3);
    private static final Duration SIGNUP_EMAIL_VERIFICATION_RESEND_COOLDOWN = Duration.ofSeconds(10);
    private static final int SIGNUP_EMAIL_VERIFICATION_RESEND_LIMIT = 3;
    private static final SecureRandom SIGNUP_EMAIL_VERIFICATION_RANDOM = new SecureRandom();

    private final UserAccountRepositoryPort userAccountRepository;
    private final LoginContextResolver loginContextResolver;
    private final PasswordHasherPort passwordHasher;
    private final AuthTokenIssuer authTokenIssuer;
    private final AccessTokenPort accessTokenPort;
    private final MembershipPort membershipPort;
    private final SignUpEmailVerificationRepositoryPort signUpEmailVerificationRepository;
    private final NotificationOutboxRepositoryPort notificationOutboxRepository;
    private final Clock clock;
    private final String fixedVerificationCode;

    public AuthApplicationService(
        UserAccountRepositoryPort userAccountRepository,
        LoginContextResolver loginContextResolver,
        PasswordHasherPort passwordHasher,
        AuthTokenIssuer authTokenIssuer,
        AccessTokenPort accessTokenPort,
        MembershipPort membershipPort,
        SignUpEmailVerificationRepositoryPort signUpEmailVerificationRepository,
        NotificationOutboxRepositoryPort notificationOutboxRepository,
        Clock clock,
        @Value("${app.user-auth.signup-email-verification.fixed-code:}") String fixedVerificationCode
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginContextResolver = loginContextResolver;
        this.passwordHasher = passwordHasher;
        this.authTokenIssuer = authTokenIssuer;
        this.accessTokenPort = accessTokenPort;
        this.membershipPort = membershipPort;
        this.signUpEmailVerificationRepository = signUpEmailVerificationRepository;
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.clock = clock;
        this.fixedVerificationCode = fixedVerificationCode == null ? "" : fixedVerificationCode.trim();
    }

    @Override
    public SignUpResult signUp(SignUpCommand command) {
        String normalizedEmail = Email.of(command.email()).value();

        userAccountRepository.findByEmail(normalizedEmail)
                .ifPresent(account -> {
                    throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_EMAIL, "Email already exists");
                });

        SignUpEmailVerification verification = signUpEmailVerificationRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_REQUIRED,
                "회원가입 전에 이메일 인증이 필요합니다"
            ));

        Instant now = Instant.now(clock);
        if (!verification.isVerifiedAvailable(now)) {
            throw new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_REQUIRED,
                "회원가입 전에 이메일 인증이 필요합니다"
            );
        }

        String passwordHash = passwordHasher.hash(command.password());
        UserAccount userAccount = UserAccount.register(normalizedEmail, command.phone(), passwordHash);
        UserAccount saved = userAccountRepository.save(userAccount);

        verification.consume(now);
        signUpEmailVerificationRepository.save(verification);

        return new SignUpResult(saved.userId());
    }

    @Override
    public SignUpEmailVerificationResult requestSignUpEmailVerification(String email) {
        String normalizedEmail = Email.of(email).value();
        userAccountRepository.findByEmail(normalizedEmail)
            .ifPresent(account -> {
                throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_EMAIL, "Email already exists");
            });

        Instant now = Instant.now(clock);
        String rawCode = generateVerificationCode();
        String codeHash = passwordHasher.hash(rawCode);

        SignUpEmailVerification verification = signUpEmailVerificationRepository.findByEmail(normalizedEmail)
            .map(existing -> {
                existing.regenerate(
                    codeHash,
                    now,
                    SIGNUP_EMAIL_VERIFICATION_TTL,
                    SIGNUP_EMAIL_VERIFICATION_RESEND_COOLDOWN,
                    SIGNUP_EMAIL_VERIFICATION_RESEND_LIMIT
                );
                return existing;
            })
            .orElseGet(() -> SignUpEmailVerification.issue(normalizedEmail, codeHash, now, SIGNUP_EMAIL_VERIFICATION_TTL));

        SignUpEmailVerification saved = signUpEmailVerificationRepository.save(verification);
        notificationOutboxRepository.save(
            NotificationOutbox.create(
                NotificationType.SIGNUP_EMAIL_VERIFICATION,
                saved.email(),
                new SignUpEmailVerificationNotificationPayload(
                    saved.verificationId(),
                    saved.email(),
                    rawCode,
                    SIGNUP_EMAIL_VERIFICATION_TTL.toSeconds()
                ),
                now
            )
        );

        return toVerificationResult(saved);
    }

    @Override
    public SignUpEmailVerificationResult confirmSignUpEmailVerification(String email, String code) {
        String normalizedEmail = Email.of(email).value();
        SignUpEmailVerification verification = signUpEmailVerificationRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UserAuthDomainException(
                UserAuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND,
                "이메일 인증 요청을 찾을 수 없습니다"
            ));

        verification.verify(code, passwordHasher::matches, Instant.now(clock));
        SignUpEmailVerification saved = signUpEmailVerificationRepository.save(verification);
        return toVerificationResult(saved);
    }

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

    private SignUpEmailVerificationResult toVerificationResult(SignUpEmailVerification verification) {
        return new SignUpEmailVerificationResult(
            verification.verificationId(),
            verification.email(),
            verification.expiresAt(),
            verification.verifiedAt(),
            verification.resendCount(),
            SIGNUP_EMAIL_VERIFICATION_RESEND_LIMIT,
            SIGNUP_EMAIL_VERIFICATION_RESEND_COOLDOWN.toSeconds()
        );
    }

    private String generateVerificationCode() {
        if (!fixedVerificationCode.isBlank()) {
            return fixedVerificationCode;
        }
        int number = SIGNUP_EMAIL_VERIFICATION_RANDOM.nextInt(100_000_000);
        return String.format("%08d", number);
    }
}
