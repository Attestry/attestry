package io.attestry.userauth.application.port.membership;

public interface MembershipPermissionProjectionRefreshPort {

    void refreshMembership(String membershipId);
}
