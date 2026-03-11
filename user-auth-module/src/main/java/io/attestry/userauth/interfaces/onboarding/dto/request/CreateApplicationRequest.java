package io.attestry.userauth.interfaces.onboarding.dto.request;

//TODO("country는 enum으로 매핑 ")
public record CreateApplicationRequest(
    String type,
    String orgName,
    String country,
    String address,
    String bizRegNo,
    String evidenceBundleId
) {
}
