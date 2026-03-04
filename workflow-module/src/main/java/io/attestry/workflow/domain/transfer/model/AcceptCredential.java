package io.attestry.workflow.domain.transfer.model;

public record AcceptCredential(
    AcceptMethod method,
    String qrNonce,
    String codeHash,
    String codeSalt
) {

    public static AcceptCredential ofQr(String nonce) {
        return new AcceptCredential(AcceptMethod.QR, nonce, null, null);
    }

    public static AcceptCredential ofCode(String hash, String salt) {
        return new AcceptCredential(AcceptMethod.CODE, null, hash, salt);
    }
}
