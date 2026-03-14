package io.attestry.userauth.infrastructure.auth;

import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class Sha256PasswordHasher implements PasswordHasherPort {

    @Override
    public String hash(String rawPassword) {
        return digest(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return digest(rawPassword).equals(hashedPassword);
    }

    private String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
