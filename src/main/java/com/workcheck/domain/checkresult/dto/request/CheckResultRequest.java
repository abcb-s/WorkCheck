package com.workcheck.domain.checkresult.dto.request;

import com.workcheck.domain.checkresult.dto.response.RuleCheckResultResponse;
import com.workcheck.global.data.FileType;
import java.util.List;

public record CheckResultRequest(
        Long assignmentId,
        String fileName,
        FileType fileType,
        List<RuleCheckResultResponse> ruleResults
) {

    public CheckResultRequest {
        ruleResults = List.copyOf(ruleResults);
    }
}
