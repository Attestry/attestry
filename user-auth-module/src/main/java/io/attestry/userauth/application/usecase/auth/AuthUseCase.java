package io.attestry.userauth.application.usecase.auth;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.result.SignUpResult;
import io.attestry.userauth.application.dto.result.VerifyPhoneResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.security.AuthPrincipal;

public interface AuthUseCase {
    SignUpResult signUp(SignUpCommand command);

    AuthTokenResult login(LoginCommand command);

    void logout(String accessToken);

    AuthPrincipal authenticate(String accessToken);

    VerifyPhoneResult verifyPhone(String userId);

    AuthTokenResult reissueToken(String userId, String tenantId);
}
