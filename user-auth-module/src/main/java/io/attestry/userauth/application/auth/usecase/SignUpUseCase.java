package io.attestry.userauth.application.auth.usecase;

import io.attestry.userauth.application.auth.command.SignUpCommand;
import io.attestry.userauth.application.auth.result.SignUpEmailVerificationResult;
import io.attestry.userauth.application.auth.result.SignUpResult;

public interface SignUpUseCase {
    SignUpResult signUp(SignUpCommand command);

    SignUpEmailVerificationResult requestSignUpEmailVerification(String email);

    SignUpEmailVerificationResult confirmSignUpEmailVerification(String email, String code);
}
