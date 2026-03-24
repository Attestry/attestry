package io.attestry.userauth.application.auth.usecase;

import io.attestry.userauth.application.auth.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.auth.view.MyAccountView;

public interface MyAccountQueryUseCase {

    MyAccountView getMyAccount(String userId);

    MyAccountView updateMyAccount(String userId, UpdateMyAccountCommand command);
}
