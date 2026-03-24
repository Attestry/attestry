package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.domain.membership.model.MembershipRole;

public record InviteCommand(String email, MembershipRole role) {
}
