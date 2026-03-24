package io.attestry.userauth.application.auth.query;

import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.application.port.identity.UserAccountReadPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountQueryService implements UserAccountReadPort {

    private final UserAccountRepositoryPort userAccountRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getEmailMapByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<String> distinctUserIds = userIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (distinctUserIds.isEmpty()) {
            return Map.of();
        }
        return userAccountRepositoryPort.findByIds(distinctUserIds).stream()
            .collect(LinkedHashMap::new,
                (map, user) -> map.put(user.userId(), user.email().value()),
                LinkedHashMap::putAll);
    }
}
