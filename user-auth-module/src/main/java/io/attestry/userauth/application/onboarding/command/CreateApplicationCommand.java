package io.attestry.userauth.application.onboarding.command;

public record CreateApplicationCommand(
    String type,
    String orgName,
    String country,
    String address,
    String bizRegNo,
    String evidenceBundleId
) {
}
