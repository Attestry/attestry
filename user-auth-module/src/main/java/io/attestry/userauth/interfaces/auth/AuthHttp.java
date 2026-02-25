package io.attestry.userauth.interfaces.auth;

import io.attestry.userauth.application.auth.AuthApplicationService;
import io.attestry.userauth.application.dto.AuthTokenResult;
import io.attestry.userauth.application.dto.LoginCommand;
import io.attestry.userauth.application.dto.SignUpCommand;
import io.attestry.userauth.interfaces.auth.dto.request.LoginRequest;
import io.attestry.userauth.interfaces.auth.dto.request.SignUpRequest;
import io.attestry.userauth.interfaces.auth.dto.response.SignUpResponse;
import io.attestry.userauth.security.AuthPrincipalResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthHttp {

    private final AuthApplicationService authApplicationService;

    public AuthHttp(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signup(@RequestBody SignUpRequest request) {
        String userId = authApplicationService.signUp(
                new SignUpCommand(
                        request.email(),
                        request.password(),
                        request.phone()
                )
        );
        return new SignUpResponse(userId);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        AuthTokenResult result = authApplicationService.login(
            new LoginCommand(request.email(), request.password(), request.tenantId(), request.groupId())
        );

        return new LoginResponse(
            result.accessToken(),
            result.tokenType(),
            result.expiresAt(),
            result.userId(),
            result.tenantId(),
            result.groupId()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        authApplicationService.logout(AuthPrincipalResolver.resolveAccessToken(authentication));
    }

    @PostMapping("/verify-phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyPhone(Authentication authentication) {
        authApplicationService.verifyPhone(AuthPrincipalResolver.resolve(authentication).userId());
    }


    public record LoginResponse(
        String accessToken,
        String tokenType,
        java.time.Instant expiresAt,
        String userId,
        String tenantId,
        String groupId
    ) {
    }
}
