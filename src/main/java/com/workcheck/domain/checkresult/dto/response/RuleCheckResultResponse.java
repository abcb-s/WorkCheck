package com.workcheck.domain.checkresult.dto.response;

import com.workcheck.domain.checkresult.type.CheckRuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "규칙별 검사 결과")
public record RuleCheckResultResponse(
        @Schema(description = "과제에 등록된 검사 규칙 식별자", example = "10")
        Long ruleId,

        @Schema(description = "검사 규칙 유형", example = "ZIP_REQUIRED_FILE")
        CheckRuleType ruleType,

        @Schema(description = "해당 규칙 통과 여부", example = "false")
        boolean passed,

        @Schema(description = "규칙을 통과하지 못했을 때 누락된 파일 또는 문구 목록",
                example = "[\"src/main/java/Main.java\"]")
        List<String> missingItems
) {

    public RuleCheckResultResponse {
        missingItems = List.copyOf(missingItems);
    }
}
