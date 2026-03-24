package io.attestry.userauth.domain.auth.policy;

@FunctionalInterface
public interface PasswordMatcher {
    boolean matches(String rawPassword, String passwordHash);
}
