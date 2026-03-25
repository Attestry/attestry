package io.attestry.workflow.infrastructure.integration.userauth;

import io.attestry.userauth.application.port.identity.UserAccountReadPort;
import io.attestry.workflow.application.port.common.UserReadPort;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class UserAuthUserReadAdapter implements UserReadPort {

    private final UserAccountReadPort userAccountReadPort;

    public UserAuthUserReadAdapter(UserAccountReadPort userAccountReadPort) {
        this.userAccountReadPort = userAccountReadPort;
    }

    @Override
    public Map<String, String> findEmailMapByUserIds(List<String> userIds) {
        return userAccountReadPort.getEmailMapByUserIds(userIds);
    }
}
