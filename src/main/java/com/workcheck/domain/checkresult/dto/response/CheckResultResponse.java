package com.workcheck.domain.checkresult.dto.response;

import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.FileType;
import java.util.List;

public record CheckResultResponse(
        Long assignmentId,
        String fileName,
        FileType fileType,
        CheckStatus status,
        List<RuleCheckResultResponse> results
) {

    public CheckResultResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }
}