package io.attestry.userauth.domain.auth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.UserAuthErrorCode;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    void assertPasswordMatchesShouldFailWhenPasswordInvalid() {
        UserAccount account = UserAccount.register("test@example.com", "010", "hashed:pw");

        UserAuthDomainException ex = assertThrows(UserAuthDomainException.class,
            () -> account.assertPasswordMatches("wrong", (raw, hash) -> ("hashed:" + raw).equals(hash)));

        assertEquals(UserAuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void verifyPhoneShouldElevateVerificationLevel() {
        UserAccount account = UserAccount.register("test@example.com", "010", "hashed:pw");

        account.verifyPhone();

        assertEquals(VerificationLevel.PHONE_VERIFIED, account.verificationLevel());
    }
}
