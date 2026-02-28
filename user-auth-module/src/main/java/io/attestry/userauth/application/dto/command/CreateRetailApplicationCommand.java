package io.attestry.userauth.application.dto.command;

public record CreateRetailApplicationCommand(
    String retailName,
    String country,
    String bizRegNo,
    String evidenceBundleId
) {
}
