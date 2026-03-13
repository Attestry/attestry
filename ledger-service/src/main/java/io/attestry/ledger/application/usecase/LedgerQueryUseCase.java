package io.attestry.ledger.application.usecase;

import io.attestry.ledger.application.ledger.query.LedgerEntryView;
import java.util.List;

public interface LedgerQueryUseCase {
    List<LedgerEntryView> listByPassportId(String passportId);

    LedgerEntryView getByPassportIdAndLedgerId(String passportId, String ledgerId);
}
