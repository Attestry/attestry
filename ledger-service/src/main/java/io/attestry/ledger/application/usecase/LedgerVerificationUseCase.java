package io.attestry.ledger.application.usecase;

import io.attestry.ledger.application.ledger.verification.LedgerVerificationResult;

public interface LedgerVerificationUseCase {
    LedgerVerificationResult verifyChain(String passportId);
}
