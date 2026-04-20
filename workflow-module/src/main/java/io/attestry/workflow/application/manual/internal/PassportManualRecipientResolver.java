package io.attestry.workflow.application.manual.internal;

import io.attestry.workflow.application.port.common.UserReadPort;
import io.attestry.workflow.domain.WorkflowDomainException;
import io.attestry.workflow.domain.WorkflowErrorCode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PassportManualRecipientResolver {

    private final UserReadPort userReadPort;

    public String resolveRecipientEmail(String ownerUserId) {
        Map<String, String> emailMap = userReadPort.findEmailMapByUserIds(List.of(ownerUserId));
        String email = emailMap.get(ownerUserId);
        if (email == null || email.isBlank()) {
            throw new WorkflowDomainException(
                WorkflowErrorCode.PASSPORT_MANUAL_RECIPIENT_NOT_FOUND,
                "Cannot find current owner's email information."
            );
        }
        return email;
    }

    public String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
