package io.attestry.userauth.domain.onboarding.model;

import java.util.UUID;

public record ApplicationId(String value) {
    public static ApplicationId of(String value) { return new ApplicationId(value); }
    public static ApplicationId generate() { return new ApplicationId(UUID.randomUUID().toString()); }
    @Override public String toString() { return value; }
}
