package com.workcheck.domain.checkresult.dto.request;

import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CheckResultRequest(
        @NotNull(message = "과제 ID를 입력해 주세요.")
        Long assignmentId,

        @NotBlank(message = "파일 이름을 입력해 주세요.")
        String fileName,

        @NotNull(message = "파일 형식을 선택해 주세요.")
        FileType fileType,

        @NotEmpty(message = "검사 결과 목록을 입력해 주세요.")
        List<@NotNull(message = "검사 결과 항목은 비어 있을 수 없어요.") @Valid RuleResultRequest> ruleResults
) {

    public CheckResultRequest {
        ruleResults = ruleResults == null ? List.of() : List.copyOf(ruleResults);
    }

    public record RuleResultRequest(
            @NotNull(message = "규칙 종류를 선택해 주세요.")
            RuleType type,

            @NotBlank(message = "기대 값을 입력해 주세요.")
            String expected,

            @NotNull(message = "검사 상태를 입력해 주세요.")
            CheckStatus status,

            List<String> missingItems
    ) {

        public RuleResultRequest {
            missingItems = missingItems == null ? List.of() : List.copyOf(missingItems);
        }
    }
}