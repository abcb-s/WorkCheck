package com.workcheck.domain.checkresult.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workcheck.domain.checkresult.dto.request.CheckResultRequest;
import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;
import com.workcheck.domain.checkresult.dto.response.RuleCheckResultResponse;
import com.workcheck.domain.checkresult.service.impl.CheckResultServiceImpl;
import com.workcheck.global.data.FileType;
import com.workcheck.global.exception.status_code.ErrorCode;
import com.workcheck.global.data.RuleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckResultServiceImplTest {

    private final CheckResultService checkResultService = new CheckResultServiceImpl();

    @Test
    void createCheckResult_allRulesPassed_returnsPassedResult() {
        List<RuleCheckResultResponse> ruleResults = List.of(
                new RuleCheckResultResponse(1L, RuleType.FILE_NAME_SUFFIX, true, List.of()),
                new RuleCheckResultResponse(2L, RuleType.ZIP_REQUIRED_PATH, true, List.of()));

        CheckResultResponse response = checkResultService.createCheckResult(new CheckResultRequest(
                1L, "20231234_홍길동.zip", FileType.ZIP, ruleResults));

        assertThat(response.passed()).isTrue();
        assertThat(response.results()).containsExactlyElementsOf(ruleResults);
    }

    @Test
    void createCheckResult_aFailedRule_returnsFailedResultAndMissingItems() {
        List<RuleCheckResultResponse> ruleResults = List.of(
                new RuleCheckResultResponse(1L, RuleType.FILE_NAME_SUFFIX, true, List.of()),
                new RuleCheckResultResponse(2L, RuleType.ZIP_REQUIRED_PATH, false,
                        List.of("src/main/java/Main.java")));

        CheckResultResponse response = checkResultService.createCheckResult(new CheckResultRequest(
                1L, "20231234_홍길동.zip", FileType.ZIP, ruleResults));

        assertThat(response.passed()).isFalse();
        assertThat(response.results().get(1).missingItems()).containsExactly("src/main/java/Main.java");
    }

    @Test
    void createCheckResult_withoutRuleResult_throwsNoApplicableRule() {
        assertThatThrownBy(() -> checkResultService.createCheckResult(new CheckResultRequest(
                1L, "20231234_홍길동.zip", FileType.ZIP, List.of())))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NO_APPLICABLE_RULE);
    }
}
