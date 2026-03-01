package io.attestry.ledger.interfaces.ledger;

import io.attestry.ledger.application.ledger.query.LedgerEntryView;
import io.attestry.ledger.application.ledger.verification.LedgerVerificationResult;
import io.attestry.ledger.application.usecase.LedgerQueryUseCase;
import io.attestry.ledger.application.usecase.LedgerVerificationUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ledgers")
public class LedgerHttp {

    private final LedgerQueryUseCase ledgerQueryUseCase;
    private final LedgerVerificationUseCase ledgerVerificationUseCase;

    public LedgerHttp(
        LedgerQueryUseCase ledgerQueryUseCase,
        LedgerVerificationUseCase ledgerVerificationUseCase
    ) {
        this.ledgerQueryUseCase = ledgerQueryUseCase;
        this.ledgerVerificationUseCase = ledgerVerificationUseCase;
    }

    @GetMapping("/passports/{passportId}/entries")
    public List<LedgerEntryResponse> listByPassport(@PathVariable("passportId") String passportId) {
        return ledgerQueryUseCase.listByPassportId(passportId).stream()
            .map(LedgerEntryResponse::from)
            .toList();
    }

    @GetMapping("/passports/{passportId}/entries/{ledgerId}")
    public LedgerEntryResponse getById(
        @PathVariable("passportId") String passportId,
        @PathVariable("ledgerId") String ledgerId
    ) {
        return LedgerEntryResponse.from(ledgerQueryUseCase.getByPassportIdAndLedgerId(passportId, ledgerId));
    }

    @GetMapping("/passports/{passportId}/verify")
    public LedgerVerificationResponse verify(@PathVariable("passportId") String passportId) {
        LedgerVerificationResult result = ledgerVerificationUseCase.verifyChain(passportId);
        return new LedgerVerificationResponse(
            result.passportId(),
            result.valid(),
            result.totalEntries(),
            result.failedSeq(),
            result.reason(),
            result.latestEntryHash()
        );
    }

    public record LedgerEntryResponse(
        String ledgerId,
        String passportId,
        long seq,
        EventResponse event,
        ActorResponse actor,
        Instant occurredAt,
        Map<String, Object> payload,
        IntegrityResponse integrity
    ) {
        static LedgerEntryResponse from(LedgerEntryView view) {
            return new LedgerEntryResponse(
                view.ledgerId(),
                view.passportId(),
                view.seq(),
                new EventResponse(view.eventCategory(), view.eventAction()),
                new ActorResponse(view.actorRole(), view.actorId()),
                view.occurredAt(),
                view.payload(),
                new IntegrityResponse(view.dataHash(), view.prevHash(), view.entryHash(), null)
            );
        }
    }

    public record EventResponse(String category, String action) {
    }

    public record ActorResponse(String role, String id) {
    }

    public record IntegrityResponse(String dataHash, String prevHash, String entryHash, String signature) {
    }

    public record LedgerVerificationResponse(
        String passportId,
        boolean valid,
        long totalEntries,
        Long failedSeq,
        String reason,
        String latestEntryHash
    ) {
    }
}
