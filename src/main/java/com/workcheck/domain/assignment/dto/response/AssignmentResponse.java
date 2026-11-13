package com.workcheck.domain.assignment.dto.response;

import com.workcheck.domain.assignment.entity.Assignment;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentResponse(
        Long id,
        String title,
        List<CheckRuleResponse> rules,
        LocalDateTime createdAt
) {

    public static AssignmentResponse from(Assignment assignment) {
        List<CheckRuleResponse> rules = assignment.getRules().stream()
                .map(CheckRuleResponse::from)
                .toList();
        return new AssignmentResponse(assignment.getId(), assignment.getTitle(), rules, assignment.getCreatedAt());
    }
}
