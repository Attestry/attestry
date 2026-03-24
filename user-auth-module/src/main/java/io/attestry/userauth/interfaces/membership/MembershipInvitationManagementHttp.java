package io.attestry.userauth.interfaces.membership;

import io.attestry.commonlib.web.CurrentActor;
import io.attestry.commonlib.infrastructure.ApiResponse;
import io.attestry.userauth.application.common.ActorContext;
import io.attestry.userauth.application.membership.command.InviteCommand;
import io.attestry.userauth.application.membership.result.InvitationResult;
import io.attestry.userauth.application.membership.result.MembershipResult;
import io.attestry.userauth.application.membership.usecase.MembershipCommandUseCase;
import io.attestry.userauth.interfaces.membership.dto.request.InviteRequest;
import io.attestry.userauth.interfaces.membership.dto.response.InvitationResponse;
import io.attestry.userauth.interfaces.membership.dto.response.MembershipResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class MembershipInvitationManagementHttp {

    private final MembershipCommandUseCase membershipCommandUseCase;

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_TENANT_INVITATION_CREATE')")
    public ApiResponse<InvitationResponse> invite(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody InviteRequest request) {
        InvitationResult result = membershipCommandUseCase.invite(
                actor,
                new InviteCommand(request.email(), request.role()));
        return ApiResponse.success(InvitationResponse.from(result));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ApiResponse<MembershipResponse> acceptInvitation(
            @CurrentActor ActorContext actor,
            @PathVariable("invitationId") String invitationId) {
        MembershipResult result = membershipCommandUseCase.acceptInvitation(actor, invitationId);
        return ApiResponse.success(MembershipResponse.from(result));
    }
}
