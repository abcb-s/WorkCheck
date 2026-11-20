package com.workcheck.domain.check.dto.response;

import com.workcheck.domain.check.entity.CheckResult;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.RuleType;

public record CheckResultResponse(
        Long id,
        RuleType type,
        String expected,
        CheckStatus status,
        String message
) {

    public static CheckResultResponse from(CheckResult result) {
        return new CheckResultResponse(result.getId(), result.getType(), result.getExpected(),
                result.getStatus(), result.getMessage());
    }
}
