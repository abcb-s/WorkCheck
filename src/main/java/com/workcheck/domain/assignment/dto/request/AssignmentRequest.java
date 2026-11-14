package com.workcheck.domain.assignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AssignmentRequest(
        @NotBlank(message = "과제명을 입력해 주세요.")
        @Size(max = 100, message = "과제명은 100자 이하로 입력해 주세요.")
        String title,

        @NotEmpty(message = "검사 규칙을 한 개 이상 등록해 주세요.")
        List<@NotNull(message = "검사 규칙은 비어 있을 수 없어요.") @Valid CheckRuleRequest> rules
) {
}
