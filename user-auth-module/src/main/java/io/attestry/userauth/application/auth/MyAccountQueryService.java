package io.attestry.userauth.application.auth;

import io.attestry.userauth.application.dto.command.UpdateMyAccountCommand;
import io.attestry.userauth.application.dto.view.MyAccountView;
import io.attestry.userauth.application.port.PasswordHasherPort;
import io.attestry.userauth.application.usecase.auth.MyAccountQueryUseCase;
import io.attestry.userauth.common.error.DomainException;
import io.attestry.userauth.common.error.ErrorCode;
import io.attestry.userauth.domain.identity.model.UserAccount;
import io.attestry.userauth.domain.identity.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyAccountQueryService implements MyAccountQueryUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasherPort passwordHasherPort;

    public MyAccountQueryService(
        UserAccountRepository userAccountRepository,
        PasswordHasherPort passwordHasherPort
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    @Transactional(readOnly = true)
    public MyAccountView getMyAccount(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return MyAccountView.from(account);
    }

    @Override
    @Transactional
    public MyAccountView updateMyAccount(String userId, UpdateMyAccountCommand command) {
        UserAccount account = userAccountRepository.findById(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "User not found"));
        account.checkActiveStatus();

        if (command.phone() != null) {
            account.updatePhone(command.phone().isBlank() ? null : command.phone().trim());
        }

        if (command.newPassword() != null && !command.newPassword().isBlank()) {
            if (command.currentPassword() == null || command.currentPassword().isBlank()) {
                throw new DomainException(ErrorCode.INVALID_CREDENTIALS, "Current password is required to change password");
            }
            account.changePassword(command.currentPassword(), passwordHasherPort.hash(command.newPassword()), passwordHasherPort::matches);
        }

        UserAccount saved = userAccountRepository.save(account);
        return MyAccountView.from(saved);
    }
}
