package io.attestry.userauth.application.membership;

import io.attestry.userauth.domain.membership.repository.MembershipRepository;
import io.attestry.userauth.domain.organization.event.GroupStatusChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GroupStatusChangedEventHandler {

    private final MembershipRepository membershipRepository;

    public GroupStatusChangedEventHandler(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @EventListener
    public void onGroupStatusChanged(GroupStatusChangedEvent event) {
        membershipRepository.updateGroupStatusOnMemberships(event.groupId(), event.newStatus());
    }
}
