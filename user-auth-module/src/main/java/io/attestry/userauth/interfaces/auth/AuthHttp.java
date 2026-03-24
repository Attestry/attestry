package io.attestry.userauth.interfaces.auth;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.auth.command.LoginCommand;
import io.attestry.userauth.application.auth.command.ResetPasswordCommand;
import io.attestry.userauth.application.auth.command.SignUpCommand;
import io.attestry.userauth.application.auth.result.AuthTokenResult;
import io.attestry.userauth.application.auth.result.SignUpEmailVerificationResult;
import io.attestry.userauth.application.auth.result.SignUpResult;
import io.attestry.userauth.application.auth.usecase.AuthUseCase;
import io.attestry.userauth.application.auth.usecase.SignUpUseCase;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.interfaces.auth.dto.request.ConfirmSignUpEmailVerificationRequest;
import io.attestry.userauth.interfaces.auth.dto.request.LoginRequest;
import io.attestry.userauth.interfaces.auth.dto.request.ResetPasswordRequest;
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
    private final SignUpUseCase signUpApplicationService;

    @PostMapping("/signup/email-verifications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpEmailVerificationResponse> requestSignUpEmailVerification(
        @Valid @RequestBody SendSignUpEmailVerificationRequest request
    ) {
        SignUpEmailVerificationResult result = signUpApplicationService.requestSignUpEmailVerification(request.email());
        return ApiResponse.success(SignUpEmailVerificationResponse.from(result));
    }

    @PostMapping("/signup/email-verifications/confirm")
    public ApiResponse<SignUpEmailVerificationResponse> confirmSignUpEmailVerification(
        @Valid @RequestBody ConfirmSignUpEmailVerificationRequest request
    ) {
        SignUpEmailVerificationResult result = signUpApplicationService.confirmSignUpEmailVerification(
            request.email(),
            request.code()
        );
        return ApiResponse.success(SignUpEmailVerificationResponse.from(result));
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
        SignUpResult result = signUpApplicationService.signUp(
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

    //TODO("비밀번호 찾기 기능 추가")
    //TODO("ApiResponse<Void>를 반환하는 대신 별도의 응답 객체를 만들어서 반환하도록 변경 고려")
    @PostMapping("/password-reset")
    public ApiResponse<Void> resetPassword(
        @CurrentActor ActorContext actor,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        authApplicationService.resetPassword(
            actor.userId(),
            new ResetPasswordCommand(request.currentPassword(), request.newPassword())
        );
        return ApiResponse.success(null);
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
