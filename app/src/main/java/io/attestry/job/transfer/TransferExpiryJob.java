package io.attestry.job.transfer;

import io.attestry.workflow.domain.transfer.model.TokenTransfer;
import io.attestry.workflow.domain.transfer.repository.TokenTransferRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransferExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(TransferExpiryJob.class);

    private final TokenTransferRepository transferRepository;
    private final Clock clock;

    public TransferExpiryJob(TokenTransferRepository transferRepository, Clock clock) {
        this.transferRepository = transferRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expirePendingTransfers() {
        Instant now = Instant.now(clock);
        List<TokenTransfer> expired = transferRepository.findPendingExpiredBefore(now);

        if (expired.isEmpty()) {
            return;
        }

        log.info("transfer expiry job started: candidates={}", expired.size());

        int count = 0;
        for (TokenTransfer transfer : expired) {
            TokenTransfer marked = transfer.markExpired(now);
            transferRepository.save(marked);
            count++;
        }

        log.info("transfer expiry job completed: expired={}", count);
    }
}
