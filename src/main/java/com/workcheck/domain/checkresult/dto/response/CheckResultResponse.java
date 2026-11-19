package com.workcheck.domain.checkresult.dto.response;

import com.workcheck.global.data.FileType;
import java.util.List;

public record CheckResultResponse(
        Long assignmentId,
        String fileName,
        FileType fileType,
        boolean passed,
        List<RuleCheckResultResponse> results
) {

    public CheckResultResponse {
        results = List.copyOf(results);
    }
}
