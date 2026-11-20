package com.workcheck.domain.checkresult.service.impl;

import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.domain.check.entity.CheckResult;
import com.workcheck.domain.check.repository.CheckHistoryRepository;
import com.workcheck.domain.checkresult.dto.request.CheckResultRequest;
import com.workcheck.domain.checkresult.dto.request.CheckResultRequest.RuleResultRequest;
import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;
import com.workcheck.domain.checkresult.dto.response.RuleCheckResultResponse;
import com.workcheck.domain.checkresult.service.CheckResultService;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CheckResultServiceImpl implements CheckResultService {

    private final AssignmentRepository assignmentRepository;
    private final CheckHistoryRepository checkHistoryRepository;

    @Override
    public CheckResultResponse createCheckResult(Long memberId, CheckResultRequest request) {
        Assignment assignment = assignmentRepository.findById(request.assignmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        if (!assignment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        List<RuleResultRequest> ruleResults = request.ruleResults();
        if (ruleResults.isEmpty()) {
            throw new CustomException(ErrorCode.NO_APPLICABLE_RULE);
        }

        List<CheckResult> entities = ruleResults.stream()
                .map(this::toEntity)
                .toList();

        CheckHistory history = CheckHistory.builder()
                .assignment(assignment)
                .fileName(request.fileName().strip())
                .status(resolveStatus(entities))
                .build();
        entities.forEach(history::addResult);
        CheckHistory saved = checkHistoryRepository.save(history);

        List<RuleCheckResultResponse> responses = IntStream.range(0, saved.getResults().size())
                .mapToObj(i -> RuleCheckResultResponse.from(
                        saved.getResults().get(i),
                        ruleResults.get(i).missingItems()))
                .toList();

        return new CheckResultResponse(
                saved.getAssignment().getId(),
                saved.getFileName(),
                request.fileType(),
                saved.getStatus(),
                responses
        );
    }

    private CheckResult toEntity(RuleResultRequest item) {
        String expected = item.expected().strip();
        return CheckResult.builder()
                .type(item.type())
                .expected(expected)
                .status(item.status())
                .message(buildMessage(item, expected))
                .build();
    }

    private String buildMessage(RuleResultRequest item, String expected) {
        CheckStatus status = item.status();
        List<String> missingItems = item.missingItems();

        return switch (item.type()) {
            case FILE_NAME_SUFFIX -> switch (status) {
                case PASS -> "파일 이름이 " + expected + " 형식으로 끝나요.";
                case FAIL -> "파일 이름이 " + expected + " 형식으로 끝나지 않아요.";
                case REVIEW -> "파일 이름(" + expected + ")은 직접 확인이 필요해요.";
            };
            case ZIP_REQUIRED_PATH -> switch (status) {
                case PASS -> "압축 파일에서 " + expected + " 경로를 찾았어요.";
                case FAIL -> {
                    String target = missingItems.isEmpty() ? expected : String.join(", ", missingItems);
                    yield "압축 파일에서 " + target + " 경로를 찾지 못했어요.";
                }
                case REVIEW -> "압축 파일의 " + expected + " 경로는 직접 확인이 필요해요.";
            };
            case PDF_REQUIRED_TEXT -> switch (status) {
                case PASS -> "PDF에서 '" + expected + "' 문구를 찾았어요.";
                case FAIL -> {
                    String target = missingItems.isEmpty() ? expected : String.join(", ", missingItems);
                    yield "PDF에서 '" + target + "' 문구를 찾지 못했어요.";
                }
                case REVIEW -> "PDF에서 글자를 읽을 수 없어요. 스캔본이라면 직접 확인해 주세요.";
            };
        };
    }

    private CheckStatus resolveStatus(List<CheckResult> results) {
        if (results.stream().anyMatch(result -> result.getStatus() == CheckStatus.FAIL)) {
            return CheckStatus.FAIL;
        }
        if (results.stream().allMatch(result -> result.getStatus() == CheckStatus.PASS)) {
            return CheckStatus.PASS;
        }
        return CheckStatus.REVIEW;
    }
}