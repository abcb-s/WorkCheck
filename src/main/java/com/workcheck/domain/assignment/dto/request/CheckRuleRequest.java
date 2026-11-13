package com.workcheck.domain.assignment.dto.request;

import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckRuleRequest(
        @NotNull(message = "파일 형식을 선택해 주세요.")
        FileType fileType,

        @NotNull(message = "검사 규칙 종류를 선택해 주세요.")
        RuleType type,

        @NotBlank(message = "검사할 값을 입력해 주세요.")
        @Size(max = 500, message = "검사할 값은 500자 이하로 입력해 주세요.")
        String value
) {
}
