package io.attestry.userauth.domain.onboarding.model;

import java.util.UUID;

public record EvidenceBundleId(String value) {
    public static EvidenceBundleId of(String value) { return new EvidenceBundleId(value); }
    public static EvidenceBundleId generate() { return new EvidenceBundleId(UUID.randomUUID().toString()); }
    @Override public String toString() { return value; }
}
