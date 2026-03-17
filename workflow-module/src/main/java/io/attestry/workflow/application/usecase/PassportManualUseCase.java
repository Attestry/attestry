package io.attestry.workflow.application.usecase;

import io.attestry.userauth.security.AuthPrincipal;
import io.attestry.workflow.application.manual.command.SendPassportManualCommand;
import io.attestry.workflow.application.manual.result.PassportManualRecipientResult;
import io.attestry.workflow.application.manual.result.SendPassportManualResult;

public interface PassportManualUseCase {

    PassportManualRecipientResult getRecipient(AuthPrincipal principal, String tenantId, String passportId);

    SendPassportManualResult send(AuthPrincipal principal, String tenantId, String passportId, SendPassportManualCommand command);
}
