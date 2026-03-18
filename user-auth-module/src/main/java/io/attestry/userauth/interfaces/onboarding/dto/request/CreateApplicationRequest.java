package io.attestry.userauth.interfaces.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

//TODO("country는 enum으로 매핑 ")
public record CreateApplicationRequest(
        @NotBlank(message = "신청 유형은 필수입니다")
        String type,
        @NotBlank(message = "조직명은 필수입니다")
        String orgName,
        @NotBlank(message = "국가는 필수입니다")
        String country,
        @NotBlank(message = "주소는 필수입니다")
        String address,
        @NotBlank(message = "사업자등록번호는 필수입니다")
        @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자 등록번호는 123-45-67890 형식으로 입력해주세요.")
        String bizRegNo,
        @NotBlank(message = "증빙 묶음 ID는 필수입니다")
        String evidenceBundleId
) {
}
