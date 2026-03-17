package io.attestry.workflow.interfaces.servicerequest.dto.request;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record SubmitServiceRequestRequest(
    String passportId,
    String providerTenantId,
    String beforeEvidenceGroupId,
    String serviceRequestMethod,
    @Size(max = 1000, message = "증상 설명은 1000자 이하로 입력해주세요.")
    String symptomDescription,
    Instant requestedReservationAt,
    @Size(max = 300, message = "연락 메모는 300자 이하로 입력해주세요.")
    String contactMemo
) {
}
