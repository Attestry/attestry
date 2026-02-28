package io.attestry.userauth.application.dto.command;

public record CreateBrandApplicationCommand(
    String brandName,
    String country,
    String bizRegNo,
    String evidenceBundleId
) {
}
