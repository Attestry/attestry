package io.attestry.workflow.application.port.common;

import java.util.List;
import java.util.Map;

public interface UserReadPort {

    Map<String, String> findEmailMapByUserIds(List<String> userIds);
}
