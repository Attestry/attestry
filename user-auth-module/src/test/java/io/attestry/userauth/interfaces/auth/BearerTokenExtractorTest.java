package io.attestry.userauth.interfaces.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class BearerTokenExtractorTest {

    @Test
    void shouldExtractBearerToken() {
        String token = BearerTokenExtractor.extract("Bearer abc.def");

        assertEquals("abc.def", token);
    }

    @Test
    void shouldRejectMissingBearerPrefix() {
        DomainException ex = assertThrows(DomainException.class, () -> BearerTokenExtractor.extract("abc.def"));

        assertEquals(ErrorCode.ACCESS_TOKEN_INVALID, ex.getErrorCode());
    }
}
