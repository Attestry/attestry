package io.attestry.userauth.application.auth.query;

import io.attestry.userauth.application.auth.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.auth.view.MyAccountView;
import io.attestry.userauth.application.port.auth.PasswordHasherPort;
import io.attestry.userauth.application.port.identity.UserAccountRepositoryPort;
import io.attestry.userauth.application.auth.usecase.MyAccountQueryUseCase;
import io.attestry.userauth.domain.UserAuthErrorCode;
import io.attestry.userauth.domain.UserAuthDomainException;
import io.attestry.userauth.domain.auth.model.UserAccount;
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
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다"));
        return MyAccountView.from(account);
    }

    @Override
    @Transactional
    public MyAccountView updateMyAccount(String userId, UpdateMyAccountCommand command) {
        UserAccount account = userAccountRepository.findById(userId)
            .orElseThrow(() -> new UserAuthDomainException(UserAuthErrorCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다"));

        String newHash = command.hasPasswordChangeRequest()
            ? passwordHasherPort.hash(command.newPassword()) : null;

        account.updateProfile(command.phone(), command.currentPassword(), newHash, passwordHasherPort::matches);

        UserAccount saved = userAccountRepository.save(account);
        return MyAccountView.from(saved);
    }
}
