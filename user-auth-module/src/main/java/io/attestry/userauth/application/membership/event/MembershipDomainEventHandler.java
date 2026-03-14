package io.attestry.userauth.application.membership.event;

import io.attestry.userauth.application.port.membership.RoleAssignmentAuditPort;
import io.attestry.userauth.domain.membership.event.RoleAssignmentAuditedEvent;
import io.attestry.userauth.domain.membership.event.TemplatePermissionMutatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipDomainEventHandler {

    private final RoleAssignmentAuditPort roleAssignmentAuditPort;

    @EventListener
    public void onRoleAssignmentAudited(RoleAssignmentAuditedEvent event) {
        roleAssignmentAuditPort.log(
            event.actorUserId(),
            event.actorTenantId(),
            event.targetMembershipId(),
            event.beforeRole(),
            event.afterRole(),
            event.decisionSource(),
            event.allowed(),
            event.reasonCode(),
            event.requestedAt(),
            event.decidedAt()
        );
    }

    @EventListener
    public void onTemplatePermissionMutated(TemplatePermissionMutatedEvent event) {
        // Reserved for async audit/notification integrations.
    }
}
