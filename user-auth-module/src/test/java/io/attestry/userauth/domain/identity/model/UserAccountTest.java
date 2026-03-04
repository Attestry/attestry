package io.attestry.userauth.domain.identity.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.identity.model.VerificationLevel;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    void assertPasswordMatchesShouldFailWhenPasswordInvalid() {
        UserAccount account = UserAccount.register("test@example.com", "010", "hashed:pw");

        DomainException ex = assertThrows(DomainException.class,
            () -> account.assertPasswordMatches("wrong", (raw, hash) -> ("hashed:" + raw).equals(hash)));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void verifyPhoneShouldElevateVerificationLevel() {
        UserAccount account = UserAccount.register("test@example.com", "010", "hashed:pw");

        account.verifyPhone();

        assertEquals(VerificationLevel.PHONE_VERIFIED, account.verificationLevel());
    }
}
