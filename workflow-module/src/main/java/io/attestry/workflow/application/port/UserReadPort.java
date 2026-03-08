package io.attestry.workflow.application.port;

import java.util.List;
import java.util.Map;

public interface UserReadPort {

    Map<String, String> findEmailsByUserIds(List<String> userIds);
}
