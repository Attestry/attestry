package io.attestry.userauth.domain.membership.model;

import java.util.UUID;

public record MembershipId(String value) {
    public static MembershipId of(String value) { return new MembershipId(value); }
    public static MembershipId generate() { return new MembershipId(UUID.randomUUID().toString()); }
    @Override public String toString() { return value; }
}
