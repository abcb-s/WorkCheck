package com.workcheck.domain.check.dto.response;

import com.workcheck.domain.check.entity.CheckResult;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.RuleType;
import java.util.List;

public record CheckResultResponse(
        Long id,
        RuleType type,
        String expected,
        CheckStatus status,
        String message,
        List<String> missingItems
) {

    public CheckResultResponse {
        missingItems = missingItems == null ? List.of() : List.copyOf(missingItems);
    }

    public static CheckResultResponse from(CheckResult result) {
        return new CheckResultResponse(result.getId(), result.getType(), result.getExpected(),
                result.getStatus(), result.getMessage(), missingItems(result));
    }

    private static List<String> missingItems(CheckResult result) {
        if (result.getStatus() != CheckStatus.FAIL || result.getType() == RuleType.FILE_NAME_SUFFIX) {
            return List.of();
        }
        return List.of(result.getExpected());
    }
}
