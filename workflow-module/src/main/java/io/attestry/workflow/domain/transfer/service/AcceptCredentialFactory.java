package io.attestry.workflow.domain.transfer.service;

import io.attestry.workflow.domain.transfer.model.AcceptCredential;
import io.attestry.workflow.domain.transfer.model.AcceptMethod;

public interface AcceptCredentialFactory {

    AcceptCredential create(AcceptMethod method, String password);
}
