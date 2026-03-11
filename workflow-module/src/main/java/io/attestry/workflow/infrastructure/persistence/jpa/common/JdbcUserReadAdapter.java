package io.attestry.workflow.infrastructure.persistence.jpa.common;

import io.attestry.workflow.application.port.common.UserReadPort;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserReadAdapter implements UserReadPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserReadAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, String> findEmailsByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        String inClause = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.query(
            """
                SELECT user_id, email
                  FROM user_accounts
                 WHERE user_id IN (%s)
            """.formatted(inClause),
            (rs, rowNum) -> Map.entry(rs.getString("user_id"), rs.getString("email")),
            userIds.toArray()
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
