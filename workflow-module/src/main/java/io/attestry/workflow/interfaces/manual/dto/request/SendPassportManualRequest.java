package io.attestry.workflow.interfaces.manual.dto.request;

import jakarta.validation.constraints.Size;

public record SendPassportManualRequest(
    @Size(max = 2000, message = "메뉴얼 입력은 2000자 이하로 작성해주세요.")
    String message,
    String evidenceGroupId
) {
}
