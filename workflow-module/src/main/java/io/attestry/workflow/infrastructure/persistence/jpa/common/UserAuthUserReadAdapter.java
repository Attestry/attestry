package io.attestry.workflow.infrastructure.persistence.jpa.common;

import io.attestry.userauth.application.usecase.identity.UserAccountQueryUseCase;
import io.attestry.workflow.application.port.common.UserReadPort;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class UserAuthUserReadAdapter implements UserReadPort {

    private final UserAccountQueryUseCase userAccountQueryUseCase;

    public UserAuthUserReadAdapter(UserAccountQueryUseCase userAccountQueryUseCase) {
        this.userAccountQueryUseCase = userAccountQueryUseCase;
    }

    @Override
    public Map<String, String> findEmailsByUserIds(List<String> userIds) {
        return userAccountQueryUseCase.getEmailsByUserIds(userIds);
    }
}
