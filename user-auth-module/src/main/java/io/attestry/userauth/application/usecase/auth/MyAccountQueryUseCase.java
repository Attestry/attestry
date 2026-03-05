package io.attestry.userauth.application.usecase.auth;

import io.attestry.userauth.application.dto.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.dto.view.MyAccountView;

public interface MyAccountQueryUseCase {

    MyAccountView getMyAccount(String userId);

    MyAccountView updateMyAccount(String userId, UpdateMyAccountCommand command);
}
