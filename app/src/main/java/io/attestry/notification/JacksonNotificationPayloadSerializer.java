package io.attestry.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.attestry.userauth.application.port.NotificationPayloadSerializerPort;
import io.attestry.userauth.domain.membership.model.InvitationNotificationPayload;
import io.attestry.userauth.domain.membership.model.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class JacksonNotificationPayloadSerializer implements NotificationPayloadSerializerPort {

    private final ObjectMapper objectMapper;

    public JacksonNotificationPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize notification payload", ex);
        }
    }

    @Override
    public Object deserialize(String json, NotificationType type) {
        try {
            return switch (type) {
                case INVITATION -> objectMapper.readValue(json, InvitationNotificationPayload.class);
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize notification payload", ex);
        }
    }
}
