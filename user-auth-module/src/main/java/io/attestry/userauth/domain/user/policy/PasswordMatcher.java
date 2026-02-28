package io.attestry.userauth.domain.user.policy;

@FunctionalInterface
public interface PasswordMatcher {
    boolean matches(String rawPassword, String passwordHash);
}
