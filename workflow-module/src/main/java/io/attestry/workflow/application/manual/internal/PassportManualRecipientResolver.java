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
                "현재 소유자의 이메일 정보를 확인할 수 없습니다."
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
