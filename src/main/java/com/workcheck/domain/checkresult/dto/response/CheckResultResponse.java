package com.workcheck.domain.checkresult.dto.response;

import com.workcheck.global.data.FileType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "제출물 검사 결과")
public record CheckResultResponse(
        @Schema(description = "검사한 과제 식별자", example = "1")
        Long assignmentId,

        @Schema(description = "검사한 제출 파일명", example = "20231234_홍길동.zip")
        String fileName,

        @Schema(description = "검사한 파일 형식", example = "ZIP")
        FileType fileType,

        @Schema(description = "모든 규칙 통과 여부", example = "false")
        boolean passed,

        @Schema(description = "규칙별 검사 결과")
        List<RuleCheckResultResponse> results
) {

    public CheckResultResponse {
        results = List.copyOf(results);
    }
}
