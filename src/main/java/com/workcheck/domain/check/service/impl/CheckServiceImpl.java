package com.workcheck.domain.check.service.impl;

import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.check.dto.response.CheckResponse;
import com.workcheck.domain.check.dto.response.CheckSummaryResponse;
import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.domain.check.entity.CheckResult;
import com.workcheck.domain.check.repository.CheckHistoryRepository;
import com.workcheck.domain.check.service.CheckService;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.FileType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import com.workcheck.global.util.FileTypeDetector;
import com.workcheck.global.util.PdfTextExtractor;
import com.workcheck.global.util.ZipInspector;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CheckServiceImpl implements CheckService {

    private final AssignmentRepository assignmentRepository;
    private final CheckHistoryRepository checkHistoryRepository;

    @Override
    public CheckResponse check(Long memberId, Long assignmentId, MultipartFile file) {
        Assignment assignment = assignmentRepository.findByIdWithRules(assignmentId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        String fileName = file.getOriginalFilename();
        byte[] content = readContent(file);
        FileType fileType = FileTypeDetector.detect(fileName, content);

        List<CheckRule> rules = assignment.getRules().stream()
                .filter(rule -> rule.getFileType() == fileType)
                .toList();
        if (rules.isEmpty()) {
            throw new CustomException(ErrorCode.NO_APPLICABLE_RULE);
        }

        List<CheckResult> results = evaluate(fileType, fileName, content, rules);
        CheckHistory history = CheckHistory.builder()
                .assignment(assignment)
                .fileName(fileName)
                .status(overallStatus(results))
                .build();
        results.forEach(history::addResult);

        return CheckResponse.from(checkHistoryRepository.save(history));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckSummaryResponse> getMyChecks(Long memberId, CheckStatus status) {
        List<CheckHistory> histories = status == null
                ? checkHistoryRepository.findAllByMemberId(memberId)
                : checkHistoryRepository.findAllByMemberIdAndStatus(memberId, status);

        return histories.stream()
                .map(CheckSummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckSummaryResponse> getChecks(Long memberId, Long assignmentId, CheckStatus status) {
        validateOwner(memberId, assignmentId);

        List<CheckHistory> histories = status == null
                ? checkHistoryRepository.findByAssignment_IdOrderByCreatedAtDescIdDesc(assignmentId)
                : checkHistoryRepository.findByAssignment_IdAndStatusOrderByCreatedAtDescIdDesc(assignmentId, status);

        return histories.stream()
                .map(CheckSummaryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckResponse getCheck(Long memberId, Long assignmentId, Long checkId) {
        validateOwner(memberId, assignmentId);

        CheckHistory history = checkHistoryRepository.findByIdWithResults(checkId, assignmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHECK_NOT_FOUND));
        return CheckResponse.from(history);
    }

    private void validateOwner(Long memberId, Long assignmentId) {
        if (!assignmentRepository.existsByIdAndMember_Id(assignmentId, memberId)) {
            throw new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        }
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_FILE, e);
        }
    }

    private List<CheckResult> evaluate(FileType fileType, String fileName, byte[] content, List<CheckRule> rules) {
        List<String> entries = fileType == FileType.ZIP ? ZipInspector.listEntries(content) : List.of();
        String text = fileType == FileType.PDF ? PdfTextExtractor.extract(content) : "";

        return rules.stream()
                .map(rule -> switch (rule.getType()) {
                    case FILE_NAME_SUFFIX -> checkFileName(rule, fileName);
                    case ZIP_REQUIRED_PATH -> checkZipPath(rule, entries);
                    case PDF_REQUIRED_TEXT -> checkPdfText(rule, text);
                })
                .toList();
    }

    private CheckResult checkFileName(CheckRule rule, String fileName) {
        if (fileName.endsWith(rule.getValue())) {
            return result(rule, CheckStatus.PASS, "파일 이름이 " + rule.getValue() + " 형식으로 끝나요.");
        }
        return result(rule, CheckStatus.FAIL, "파일 이름이 " + rule.getValue() + " 형식으로 끝나지 않아요.");
    }

    private CheckResult checkZipPath(CheckRule rule, List<String> entries) {
        if (ZipInspector.containsPath(entries, rule.getValue())) {
            return result(rule, CheckStatus.PASS, "압축 파일에서 " + rule.getValue() + " 경로를 찾았어요.");
        }
        return result(rule, CheckStatus.FAIL, "압축 파일에서 " + rule.getValue() + " 경로를 찾지 못했어요.");
    }

    private CheckResult checkPdfText(CheckRule rule, String text) {
        if (text.isEmpty()) {
            return result(rule, CheckStatus.REVIEW, "PDF에서 글자를 읽을 수 없어요. 스캔본이라면 직접 확인해 주세요.");
        }
        if (text.contains(rule.getValue())) {
            return result(rule, CheckStatus.PASS, "PDF에서 '" + rule.getValue() + "' 문구를 찾았어요.");
        }
        return result(rule, CheckStatus.FAIL, "PDF에서 '" + rule.getValue() + "' 문구를 찾지 못했어요.");
    }

    private CheckResult result(CheckRule rule, CheckStatus status, String message) {
        return CheckResult.builder()
                .type(rule.getType())
                .expected(rule.getValue())
                .status(status)
                .message(message)
                .build();
    }

    private CheckStatus overallStatus(List<CheckResult> results) {
        if (results.stream().anyMatch(result -> result.getStatus() == CheckStatus.FAIL)) {
            return CheckStatus.FAIL;
        }
        if (results.stream().anyMatch(result -> result.getStatus() == CheckStatus.REVIEW)) {
            return CheckStatus.REVIEW;
        }
        return CheckStatus.PASS;
    }
}
