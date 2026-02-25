package io.attestry.userauth.application.membership;

import io.attestry.userauth.application.port.MembershipAdminRepositoryPort;
import io.attestry.userauth.application.port.UserAccountRepositoryPort;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.auth.model.AuthPrincipal;
import io.attestry.userauth.domain.membership.model.Invitation;
import io.attestry.userauth.domain.membership.model.MembershipRole;
import io.attestry.userauth.domain.membership.model.MembershipStatus;
import io.attestry.userauth.domain.membership.model.Membership;
import io.attestry.userauth.domain.user.model.UserAccount;
import io.attestry.userauth.domain.policy.TenantIsolationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipAdminService {

    private final MembershipAdminRepositoryPort membershipAdminRepository;
    private final UserAccountRepositoryPort userAccountRepository;
    private final Clock clock;

    public MembershipAdminService(
        MembershipAdminRepositoryPort membershipAdminRepository,
        UserAccountRepositoryPort userAccountRepository,
        Clock clock
    ) {
        this.membershipAdminRepository = membershipAdminRepository;
        this.userAccountRepository = userAccountRepository;
        this.clock = clock;
    }

    @Transactional
    public Invitation invite(AuthPrincipal principal, String tenantId, InviteCommand command) {
        assertTenantIsolation(principal, tenantId);
        MembershipAdminRepositoryPort.GroupView group = membershipAdminRepository.findGroupById(command.groupId())
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Group not found"));
        if (!tenantId.equals(group.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant group access denied");
        }

        Invitation invitation = Invitation.issue(
            tenantId,
            command.groupId(),
            command.email(),
            command.role(),
            principal.userId(),
            Instant.now(clock)
        );
        return membershipAdminRepository.saveInvitation(invitation);
    }

    @Transactional
    public Membership acceptInvitation(AuthPrincipal principal, String invitationId) {
        Invitation invitation = membershipAdminRepository.findInvitationById(invitationId)
            .orElseThrow(() -> new DomainException(ErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        UserAccount userAccount = userAccountRepository.findByUserId(principal.userId())
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));

        Membership membership = membershipAdminRepository.createMembership(
            principal.userId(),
            invitation.groupId(),
            invitation.tenantId(),
            invitation.role()
        );
        membershipAdminRepository.saveInvitation(invitation.accept(principal.userId(), userAccount.user().email(), Instant.now(clock)));

        return membership;
    }

    @Transactional(readOnly = true)
    public List<Membership> listMemberships(AuthPrincipal principal, String tenantId) {
        assertTenantIsolation(principal, tenantId);
        return membershipAdminRepository.findMembershipsByTenantId(tenantId);
    }

    @Transactional
    public Membership updateMembership(
        AuthPrincipal principal,
        String tenantId,
        String membershipId,
        UpdateMembershipCommand command
    ) {
        assertTenantIsolation(principal, tenantId);
        Membership current = membershipAdminRepository.findMembershipById(membershipId)
            .orElseThrow(() -> new DomainException(ErrorCode.MEMBERSHIP_NOT_FOUND, "Membership not found"));

        if (!tenantId.equals(current.tenantId())) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant membership access denied");
        }

        return membershipAdminRepository.updateMembership(
            current.membershipId(),
            command.role() == null ? current.role() : command.role(),
            command.status() == null ? current.status() : command.status()
        );
    }

    private void assertTenantIsolation(AuthPrincipal principal, String tenantId) {
        if (!TenantIsolationPolicy.isIsolated(principal.tenantId(), tenantId)) {
            throw new DomainException(ErrorCode.TENANT_ISOLATION_VIOLATION, "Cross-tenant access denied");
        }
    }

    public record InviteCommand(String email, String groupId, MembershipRole role) {
    }

    public record UpdateMembershipCommand(MembershipRole role, MembershipStatus status) {
    }
}
