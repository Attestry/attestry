package io.attestry.product.infrastructure.service;

import io.attestry.product.domain.service.QrPublicCodeGenerator;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class DefaultQrPublicCodeGenerator implements QrPublicCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nextCode() {
        StringBuilder b = new StringBuilder("QRPUB-");
        for (int i = 0; i < 12; i++) {
            b.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return b.toString();
    }
}
