package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.dto.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.dto.view.MyAccountView;
import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.application.usecase.auth.MyAccountQueryUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.identity.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyAccountService implements MyAccountQueryUseCase {

    private final UserAccountRepositoryPort userAccountRepository;
    private final PasswordHasherPort passwordHasherPort;


    @Override
    @Transactional(readOnly = true)
    public MyAccountView getMyAccount(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));
        return MyAccountView.from(account);
    }

    @Override
    @Transactional
    public MyAccountView updateMyAccount(String userId, UpdateMyAccountCommand command) {
        UserAccount account = userAccountRepository.findById(userId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "User not found"));

        String newHash = command.hasPasswordChangeRequest()
            ? passwordHasherPort.hash(command.newPassword()) : null;

        account.updateProfile(command.phone(), command.currentPassword(), newHash, passwordHasherPort::matches);

        UserAccount saved = userAccountRepository.save(account);
        return MyAccountView.from(saved);
    }
}
