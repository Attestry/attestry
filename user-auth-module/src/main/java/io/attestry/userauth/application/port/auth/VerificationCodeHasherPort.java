package io.attestry.userauth.application.port.auth;

public interface VerificationCodeHasherPort {
    String hash(String rawCode);

    boolean matches(String rawCode, String hashedCode);
}
