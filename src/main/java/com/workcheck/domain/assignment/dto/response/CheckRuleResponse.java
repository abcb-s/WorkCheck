package com.workcheck.domain.assignment.dto.response;

import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;

public record CheckRuleResponse(
        Long id,
        FileType fileType,
        RuleType type,
        String value
) {

    public static CheckRuleResponse from(CheckRule rule) {
        return new CheckRuleResponse(rule.getId(), rule.getFileType(), rule.getType(), rule.getValue());
    }
}
