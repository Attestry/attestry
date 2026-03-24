package io.attestry.userauth.application.auth.command;

import io.attestry.userauth.application.auth.result.SignUpEmailVerificationResult;
import io.attestry.userauth.application.auth.result.SignUpResult;
import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import io.attestry.userauth.application.port.auth.SignUpEmailVerificationRepositoryPort;
import io.attestry.userauth.application.port.auth.VerificationCodeHasherPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.application.port.notification.NotificationOutboxRepositoryPort;
import io.attestry.userauth.application.auth.usecase.SignUpUseCase;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.auth.model.Email;
import io.attestry.userauth.domain.auth.model.SignUpEmailVerification;
import io.attestry.userauth.domain.auth.model.SignUpEmailVerificationNotificationPayload;
import io.attestry.userauth.domain.auth.model.UserAccount;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationType;
import io.attestry.userauth.infrastructure.config.SignUpEmailVerificationProperties;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignUpApplicationService implements SignUpUseCase {

    private static final Duration SIGNUP_EMAIL_VERIFICATION_RESEND_COOLDOWN = Duration.ofSeconds(10);
    private static final int SIGNUP_EMAIL_VERIFICATION_RESEND_LIMIT = 3;
    private static final SecureRandom SIGNUP_EMAIL_VERIFICATION_RANDOM = new SecureRandom();

    private final UserAccountRepositoryPort userAccountRepository;
    private final PasswordHasherPort passwordHasher;
    private final VerificationCodeHasherPort verificationCodeHasher;
    private final SignUpEmailVerificationRepositoryPort signUpEmailVerificationRepository;
    private final NotificationOutboxRepositoryPort notificationOutboxRepository;
    private final Clock clock;
    private final SignUpEmailVerificationProperties verificationProperties;

    @Override
    public SignUpResult signUp(SignUpCommand command) {
        String normalizedEmail = Email.of(command.email()).value();

        userAccountRepository.findByEmail(normalizedEmail)
                .ifPresent(account -> {
                    throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_EMAIL, "이미 등록된 이메일입니다");
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
                throw new UserAuthDomainException(UserAuthErrorCode.DUPLICATE_EMAIL, "이미 등록된 이메일입니다");
            });

        Instant now = Instant.now(clock);
        String rawCode = generateVerificationCode();
        String codeHash = verificationCodeHasher.hash(rawCode);
        Duration ttl = verificationProperties.getTtl();

        SignUpEmailVerification verification = signUpEmailVerificationRepository.findByEmail(normalizedEmail)
            .map(existing -> {
                existing.regenerate(
                    codeHash,
                    now,
                    ttl,
                    SIGNUP_EMAIL_VERIFICATION_RESEND_COOLDOWN,
                    SIGNUP_EMAIL_VERIFICATION_RESEND_LIMIT
                );
                return existing;
            })
            .orElseGet(() -> SignUpEmailVerification.issue(normalizedEmail, codeHash, now, ttl));

        SignUpEmailVerification saved = signUpEmailVerificationRepository.save(verification);
        notificationOutboxRepository.save(
            NotificationOutbox.create(
                NotificationType.SIGNUP_EMAIL_VERIFICATION,
                saved.email(),
                new SignUpEmailVerificationNotificationPayload(
                    saved.verificationId(),
                    saved.email(),
                    rawCode,
                    ttl.toSeconds()
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

        verification.verify(code, verificationCodeHasher::matches, Instant.now(clock));
        SignUpEmailVerification saved = signUpEmailVerificationRepository.save(verification);
        return toVerificationResult(saved);
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
        String fixedCode = verificationProperties.getFixedCode();
        if (fixedCode != null && !fixedCode.trim().isBlank()) {
            return fixedCode.trim();
        }
        int number = SIGNUP_EMAIL_VERIFICATION_RANDOM.nextInt(100_000_000);
        return String.format("%08d", number);
    }
}
