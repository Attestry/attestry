package io.attestry.userauth.application.usecase.identity;

import java.util.List;
import java.util.Map;

public interface UserAccountQueryUseCase {

    Map<String, String> getEmailsByUserIds(List<String> userIds);
}
