package io.attestry.userauth.application.port.auth;

public interface PasswordHasherPort {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
