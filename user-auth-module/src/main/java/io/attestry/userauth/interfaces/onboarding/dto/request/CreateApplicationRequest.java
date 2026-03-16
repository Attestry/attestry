package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;

//TODO("country는 enum으로 매핑 ")
public record CreateApplicationRequest(
        @NotBlank(message = "신청 유형은 필수입니다")
        String type,
        @NotBlank(message = "조직명은 필수입니다")
        String orgName,
        @NotBlank(message = "국가는 필수입니다")
        String country,
        String address,
        @NotBlank(message = "사업자등록번호는 필수입니다")
        String bizRegNo,
        @NotBlank(message = "증빙 묶음 ID는 필수입니다")
        String evidenceBundleId
) {
}
