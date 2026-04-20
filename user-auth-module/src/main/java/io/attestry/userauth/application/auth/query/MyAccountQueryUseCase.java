package io.attestry.userauth.application.auth.query;

import io.attestry.userauth.application.auth.command.UpdateMyAccountCommand;

public interface MyAccountQueryUseCase {

    MyAccountView getMyAccount(String userId);

    MyAccountView updateMyAccount(String userId, UpdateMyAccountCommand command);
}
