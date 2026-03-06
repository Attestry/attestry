package io.attestry.userauth.interfaces.membership.dto.request;

import io.attestry.userauth.domain.membership.model.MembershipRole;

public record InviteRequest(String email, MembershipRole role) {
}
