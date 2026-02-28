package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.membership.model.Membership;

public interface MembershipProvisioningRepositoryPort {
    Membership save(Membership membership);
}
