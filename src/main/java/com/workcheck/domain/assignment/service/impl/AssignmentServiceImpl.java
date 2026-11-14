package com.workcheck.domain.assignment.service.impl;

import com.workcheck.domain.assignment.dto.request.AssignmentRequest;
import com.workcheck.domain.assignment.dto.request.CheckRuleRequest;
import com.workcheck.domain.assignment.dto.response.AssignmentResponse;
import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.assignment.service.AssignmentService;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;

    @Override
    public AssignmentResponse createAssignment(AssignmentRequest request) {
        List<CheckRule> rules = createRules(request.rules());

        Assignment assignment = Assignment.builder()
                .title(request.title().strip())
                .build();

        assignment.replaceRules(rules);

        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments() {
        return assignmentRepository.findAllWithRules().stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithRules(assignmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        return AssignmentResponse.from(assignment);
    }

    @Override
    public AssignmentResponse updateAssignment(Long assignmentId, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findByIdWithRules(assignmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        List<CheckRule> rules = createRules(request.rules());

        assignment.updateTitle(request.title().strip());
        assignment.replaceRules(rules);

        return AssignmentResponse.from(assignmentRepository.saveAndFlush(assignment));
    }

    @Override
    public void deleteAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithRules(assignmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        assignmentRepository.delete(assignment);
    }

    private List<CheckRule> createRules(List<CheckRuleRequest> requests) {
        List<CheckRule> rules = new ArrayList<>();

        for (CheckRuleRequest rule : requests) {
            validateRule(rule);
            rules.add(CheckRule.builder()
                    .fileType(rule.fileType())
                    .type(rule.type())
                    .value(rule.value())
                    .build());
        }

        return rules;
    }

    private void validateRule(CheckRuleRequest rule) {
        if (rule.fileType() == FileType.ZIP && rule.type() == RuleType.PDF_REQUIRED_TEXT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (rule.fileType() == FileType.PDF && rule.type() == RuleType.ZIP_REQUIRED_PATH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
