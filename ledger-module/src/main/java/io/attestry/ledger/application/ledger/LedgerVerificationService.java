package io.attestry.ledger.application.ledger;

import io.attestry.ledger.application.ledger.verification.LedgerVerificationResult;
import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.application.usecase.LedgerVerificationUseCase;
import io.attestry.ledger.domain.ledger.model.LedgerChainVerification;
import io.attestry.ledger.domain.ledger.model.PassportId;
import io.attestry.ledger.domain.ledger.service.LedgerChainVerifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerVerificationService implements LedgerVerificationUseCase {

    private final LedgerQueryRepositoryPort repository;
    private final LedgerChainVerifier chainVerifier;

    public LedgerVerificationService(LedgerQueryRepositoryPort repository, LedgerChainVerifier chainVerifier) {
        this.repository = repository;
        this.chainVerifier = chainVerifier;
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerVerificationResult verifyChain(String passportId) {
        PassportId parsedPassportId = PassportId.of(passportId);
        LedgerChainVerification verification = chainVerifier.verify(
            parsedPassportId,
            repository.findByPassportIdOrderBySeqAsc(parsedPassportId.value())
        );
        return new LedgerVerificationResult(
            verification.passportId().value(),
            verification.valid(),
            verification.totalEntries(),
            verification.failedSeq(),
            verification.reason(),
            verification.latestEntryHash()
        );
    }
}
