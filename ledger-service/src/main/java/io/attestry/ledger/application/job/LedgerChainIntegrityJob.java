package io.attestry.ledger.application.job;

import io.attestry.ledger.application.port.LedgerQueryRepositoryPort;
import io.attestry.ledger.domain.ledger.model.LedgerChainVerification;
import io.attestry.ledger.domain.ledger.model.PassportId;
import io.attestry.ledger.domain.ledger.service.LedgerChainVerifier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.ledger.integrity-check", name = "enabled", havingValue = "true")
public class LedgerChainIntegrityJob {

    private static final Logger log = LoggerFactory.getLogger(LedgerChainIntegrityJob.class);

    private final LedgerQueryRepositoryPort queryRepository;
    private final LedgerChainVerifier chainVerifier;
    private final Counter verifySuccessCounter;
    private final Counter verifyFailureCounter;

    public LedgerChainIntegrityJob(
        LedgerQueryRepositoryPort queryRepository,
        LedgerChainVerifier chainVerifier,
        MeterRegistry meterRegistry
    ) {
        this.queryRepository = queryRepository;
        this.chainVerifier = chainVerifier;
        this.verifySuccessCounter = Counter.builder("ledger.integrity.check")
            .tag("result", "valid")
            .register(meterRegistry);
        this.verifyFailureCounter = Counter.builder("ledger.integrity.check")
            .tag("result", "invalid")
            .register(meterRegistry);
    }

    @Scheduled(cron = "${app.ledger.integrity-check.cron:0 0 3 * * *}")
    @Transactional(readOnly = true)
    public void verifyAll() {
        List<String> passportIds = queryRepository.findAllPassportIds();
        log.info("ledger integrity check started: totalPassports={}", passportIds.size());

        int validCount = 0;
        int invalidCount = 0;

        for (String passportId : passportIds) {
            LedgerChainVerification result = chainVerifier.verify(
                PassportId.of(passportId),
                queryRepository.findByPassportIdOrderBySeqAsc(passportId)
            );

            if (result.valid()) {
                verifySuccessCounter.increment();
                validCount++;
            } else {
                verifyFailureCounter.increment();
                invalidCount++;
                log.warn("ledger integrity FAILED: passportId={}, failedSeq={}, reason={}, totalEntries={}",
                    passportId, result.failedSeq(), result.reason(), result.totalEntries());
            }
        }

        log.info("ledger integrity check completed: valid={}, invalid={}, total={}",
            validCount, invalidCount, passportIds.size());
    }
}
