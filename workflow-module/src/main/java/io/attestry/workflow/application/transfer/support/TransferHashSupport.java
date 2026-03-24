package io.attestry.workflow.application.transfer.support;

import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;
import io.attestry.workflow.domain.transfer.service.AcceptCredentialFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TransferHashSupport implements AcceptCredentialFactory {

    @Override
    public AcceptCredential create(AcceptMethod method, String password) {
        if (method == AcceptMethod.QR) {
            return AcceptCredential.ofQr(UUID.randomUUID().toString());
        }
        String salt = generateSalt();
        return AcceptCredential.ofCode(hash(password, salt), salt);
    }

    public String generateSalt() {
        return UUID.randomUUID().toString();
    }

    public String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((password + salt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public boolean verify(String password, String hash, String salt) {
        return hash(password, salt).equals(hash);
    }
}
