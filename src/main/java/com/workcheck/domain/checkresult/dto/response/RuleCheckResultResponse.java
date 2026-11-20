package com.workcheck.domain.checkresult.dto.response;

import com.workcheck.domain.check.entity.CheckResult;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.RuleType;
import java.util.List;

public record RuleCheckResultResponse(
        RuleType type,
        String expected,
        CheckStatus status,
        String message,
        List<String> missingItems
) {

    public RuleCheckResultResponse {
        missingItems = missingItems == null ? List.of() : List.copyOf(missingItems);
    }

    public static RuleCheckResultResponse from(CheckResult result, List<String> missingItems) {
        return new RuleCheckResultResponse(
                result.getType(),
                result.getExpected(),
                result.getStatus(),
                result.getMessage(),
                missingItems
        );
    }
}