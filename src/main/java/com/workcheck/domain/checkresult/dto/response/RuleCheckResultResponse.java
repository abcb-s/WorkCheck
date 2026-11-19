package com.workcheck.domain.checkresult.dto.response;

import com.workcheck.global.data.RuleType;
import java.util.List;

public record RuleCheckResultResponse(
        Long ruleId,
        RuleType ruleType,
        boolean passed,
        List<String> missingItems
) {

    public RuleCheckResultResponse {
        missingItems = List.copyOf(missingItems);
    }
}
