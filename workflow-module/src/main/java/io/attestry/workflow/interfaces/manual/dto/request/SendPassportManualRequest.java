package io.attestry.workflow.interfaces.manual.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SendPassportManualRequest(
    @NotEmpty(message = "대상 passportIds는 비어 있을 수 없습니다.")
    List<String> passportIds,
    @Size(max = 2000, message = "메뉴얼 입력은 2000자 이하로 작성해주세요.")
    String message,
    String evidenceGroupId
) {
}
