package io.attestry.userauth.infrastructure.auth;

import io.attestry.userauth.application.port.auth.VerificationCodeHasherPort;
import io.attestry.userauth.infrastructure.config.SignUpEmailVerificationProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSha256VerificationCodeHasher implements VerificationCodeHasherPort {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final SecretKeySpec secretKeySpec;

    public HmacSha256VerificationCodeHasher(SignUpEmailVerificationProperties properties) {
        this.secretKeySpec = new SecretKeySpec(
            properties.getHashSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256
        );
    }

    @Override
    public String hash(String rawCode) {
        return digest(rawCode);
    }

    @Override
    public boolean matches(String rawCode, String hashedCode) {
        byte[] actual = digest(rawCode).getBytes(StandardCharsets.UTF_8);
        byte[] expected = hashedCode == null ? new byte[0] : hashedCode.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }

    private String digest(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(secretKeySpec);
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 algorithm unavailable", ex);
        }
    }
}
