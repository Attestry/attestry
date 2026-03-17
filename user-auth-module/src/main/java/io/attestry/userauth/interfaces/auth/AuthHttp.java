package io.attestry.userauth.interfaces.auth;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.dto.result.AuthTokenResult;
import io.attestry.userauth.application.dto.result.SignUpResult;
import io.attestry.userauth.application.dto.result.SignUpEmailVerificationResult;
import io.attestry.userauth.application.dto.command.LoginCommand;
import io.attestry.userauth.application.dto.command.SignUpCommand;
import io.attestry.userauth.application.usecase.auth.AuthUseCase;
import io.attestry.userauth.application.dto.command.ActorContext;
import io.attestry.userauth.interfaces.auth.dto.request.ConfirmSignUpEmailVerificationRequest;
import io.attestry.userauth.interfaces.auth.dto.request.LoginRequest;
import io.attestry.userauth.interfaces.auth.dto.request.SendSignUpEmailVerificationRequest;
import io.attestry.userauth.interfaces.auth.dto.request.SignUpRequest;
import io.attestry.userauth.interfaces.auth.dto.request.TenantSwitchRequest;
import io.attestry.userauth.interfaces.auth.dto.response.LoginResponse;
import io.attestry.userauth.interfaces.auth.dto.response.SignUpEmailVerificationResponse;
import io.attestry.userauth.interfaces.auth.dto.response.SignUpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping({ "/auth"})
public class AuthHttp {

    private final AuthUseCase authApplicationService;

    @PostMapping("/signup/email-verifications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpEmailVerificationResponse> requestSignUpEmailVerification(
        @Valid @RequestBody SendSignUpEmailVerificationRequest request
    ) {
        SignUpEmailVerificationResult result = authApplicationService.requestSignUpEmailVerification(request.email());
        return ApiResponse.success(SignUpEmailVerificationResponse.from(result));
    }

    @PostMapping("/signup/email-verifications/confirm")
    public ApiResponse<SignUpEmailVerificationResponse> confirmSignUpEmailVerification(
        @Valid @RequestBody ConfirmSignUpEmailVerificationRequest request
    ) {
        SignUpEmailVerificationResult result = authApplicationService.confirmSignUpEmailVerification(
            request.email(),
            request.code()
        );
        return ApiResponse.success(SignUpEmailVerificationResponse.from(result));
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
        SignUpResult result = authApplicationService.signUp(
                new SignUpCommand(
                        request.email(),
                        request.password(),
                        request.phone()));
        return ApiResponse.success(new SignUpResponse(result.userId()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        AuthTokenResult result = authApplicationService.login(
                new LoginCommand(request.email(), request.password(), request.tenantId()));

        return ApiResponse.success(new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresAt(),
                result.userId(),
                result.tenantId()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication) {
        authApplicationService.logout((String) authentication.getCredentials());
    }

    @PostMapping("/verify-phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyPhone(@CurrentActor ActorContext actor) {
        authApplicationService.verifyPhone(actor.userId());
    }

    //TODO("어차피 tenantId는 principal에서 가져올 수 있잖아")
    @PostMapping("/token-reissue")
    public ApiResponse<LoginResponse> reissueToken(
        @CurrentActor ActorContext actor
    ) {

        AuthTokenResult result = authApplicationService.reissueToken(
            actor.userId(),
            actor.tenantId()
        );
        return ApiResponse.success(new LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresAt(),
                result.userId(),
                result.tenantId()));
    }

    @PostMapping("/tenant-switch")
    public ApiResponse<LoginResponse> switchTenant(
        @CurrentActor ActorContext actor,
        @Valid @RequestBody TenantSwitchRequest request
    ) {
        AuthTokenResult result = authApplicationService.switchTenant(
            actor.userId(),
            request.membershipId()
        );
        return ApiResponse.success(new LoginResponse(
            result.accessToken(),
            result.tokenType(),
            result.expiresAt(),
            result.userId(),
            result.tenantId()
        ));
    }
}
