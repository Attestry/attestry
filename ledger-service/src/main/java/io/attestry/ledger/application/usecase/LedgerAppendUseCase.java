package io.attestry.ledger.application.usecase;

import io.attestry.ledger.application.ledger.command.AppendLedgerEntryCommand;
import io.attestry.ledger.application.ledger.result.AppendLedgerEntryResult;

public interface LedgerAppendUseCase {
    AppendLedgerEntryResult append(AppendLedgerEntryCommand command);
}
