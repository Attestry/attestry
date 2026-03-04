package io.attestry.userauth.interfaces.auth;

import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.result.SignUpResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.application.usecase.auth.AuthUseCase;
import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.userauth.interfaces.auth.dto.request.LoginRequest;
import io.attestry.userauth.interfaces.auth.dto.request.SignUpRequest;
import io.attestry.userauth.interfaces.auth.dto.response.LoginResponse;
import io.attestry.userauth.interfaces.auth.dto.response.SignUpResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth", "/api-v1/auth"})
public class AuthHttp {

    private final AuthUseCase authApplicationService;

    public AuthHttp(AuthUseCase authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signup(@Valid @RequestBody SignUpRequest request) {
        SignUpResult result = authApplicationService.signUp(
                new SignUpCommand(
                        request.email(),
                        request.password(),
                        request.phone()
                )
        );
        return new SignUpResponse(result.userId());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

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
        authApplicationService.logout((String) authentication.getCredentials());
    }

    @PostMapping("/verify-phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyPhone(@AuthenticationPrincipal AuthPrincipal principal) {
        authApplicationService.verifyPhone(principal.userId());
    }
}
