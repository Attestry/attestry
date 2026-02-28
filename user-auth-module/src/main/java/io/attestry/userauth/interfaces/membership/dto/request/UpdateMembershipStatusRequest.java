package io.attestry.userauth.interfaces.membership.dto.request;

import io.attestry.userauth.domain.membership.model.MembershipStatus;

public record UpdateMembershipStatusRequest(MembershipStatus status) {
}
