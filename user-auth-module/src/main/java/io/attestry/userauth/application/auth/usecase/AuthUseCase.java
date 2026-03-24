package io.attestry.userauth.application.auth.usecase;

import io.attestry.userauth.application.auth.command.LoginCommand;
import io.attestry.userauth.application.auth.command.ResetPasswordCommand;
import io.attestry.userauth.application.auth.result.AuthTokenResult;
import io.attestry.userauth.application.auth.result.VerifyPhoneResult;
import io.attestry.userauth.security.AuthPrincipal;

public interface AuthUseCase {
    AuthTokenResult login(LoginCommand command);

    void logout(String accessToken);

    AuthPrincipal authenticate(String accessToken);

    VerifyPhoneResult verifyPhone(String userId);

    AuthTokenResult reissueToken(String userId, String tenantId);

    AuthTokenResult switchTenant(String userId, String membershipId);

    void resetPassword(String userId, ResetPasswordCommand command);
}
