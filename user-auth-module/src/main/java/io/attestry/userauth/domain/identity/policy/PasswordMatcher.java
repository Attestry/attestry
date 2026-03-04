package io.attestry.userauth.domain.identity.policy;

@FunctionalInterface
public interface PasswordMatcher {
    boolean matches(String rawPassword, String passwordHash);
}
