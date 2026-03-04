package io.attestry.userauth.domain.organization.model;

import java.util.UUID;

public record GroupId(String value) {
    public static GroupId of(String value) { return new GroupId(value); }
    public static GroupId generate() { return new GroupId(UUID.randomUUID().toString()); }
    @Override public String toString() { return value; }
}
