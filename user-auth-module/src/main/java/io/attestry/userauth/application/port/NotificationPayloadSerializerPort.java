package io.attestry.userauth.application.port;

import io.attestry.userauth.domain.membership.model.NotificationType;

public interface NotificationPayloadSerializerPort {

    String serialize(Object payload);

    Object deserialize(String json, NotificationType type);
}
