package io.attestry.userauth.application.auth.command;

public interface SignUpUseCase {
    SignUpResult signUp(SignUpCommand command);

    SignUpEmailVerificationResult requestSignUpEmailVerification(String email);

    SignUpEmailVerificationResult confirmSignUpEmailVerification(String email, String code);
}
