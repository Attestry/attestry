package io.attestry.workflow.application.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.attestry.workflow.application.transfer.internal.TransferHashSupport;
import org.junit.jupiter.api.Test;

class TransferHashSupportTest {

    private final TransferHashSupport hashSupport = new TransferHashSupport();

    @Test
    void generateSalt_returnsNonNull() {
        String salt = hashSupport.generateSalt();
        assertNotNull(salt);
        assertFalse(salt.isBlank());
    }

    @Test
    void generateSalt_isUnique() {
        String salt1 = hashSupport.generateSalt();
        String salt2 = hashSupport.generateSalt();
        assertNotEquals(salt1, salt2);
    }

    @Test
    void hash_returnsSha256HexString() {
        String hash = hashSupport.hash("password", "salt");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[a-f0-9]{64}$"));
    }

    @Test
    void hash_isDeterministic() {
        String hash1 = hashSupport.hash("password", "salt");
        String hash2 = hashSupport.hash("password", "salt");
        assertEquals(hash1, hash2);
    }

    @Test
    void hash_differentSalt_producesDifferentHash() {
        String hash1 = hashSupport.hash("password", "salt1");
        String hash2 = hashSupport.hash("password", "salt2");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void verify_correctPassword_returnsTrue() {
        String salt = hashSupport.generateSalt();
        String hash = hashSupport.hash("mypassword", salt);

        assertTrue(hashSupport.verify("mypassword", hash, salt));
    }

    @Test
    void verify_wrongPassword_returnsFalse() {
        String salt = hashSupport.generateSalt();
        String hash = hashSupport.hash("mypassword", salt);

        assertFalse(hashSupport.verify("wrongpassword", hash, salt));
    }
}
