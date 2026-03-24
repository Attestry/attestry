package io.attestry.userauth.application.port.identity;

import java.util.List;
import java.util.Map;

public interface UserAccountReadPort {

    Map<String, String> getEmailMapByUserIds(List<String> userIds);
}
