package io.attestry.userauth.application.membership.command;

import io.attestry.userauth.domain.membership.model.MembershipStatus;

public record UpdateMembershipStatusCommand(MembershipStatus status) {
}
