package io.attestry.userauth.domain.membership.model;

import java.util.UUID;

public record InvitationId(String value) {
    public static InvitationId of(String value) { return new InvitationId(value); }
    public static InvitationId generate() { return new InvitationId(UUID.randomUUID().toString()); }
    @Override public String toString() { return value; }
}
