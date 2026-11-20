package com.workcheck.domain.checkresult.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.domain.check.repository.CheckHistoryRepository;
import com.workcheck.domain.checkresult.dto.request.CheckResultRequest;
import com.workcheck.domain.checkresult.dto.request.CheckResultRequest.RuleResultRequest;
import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;
import com.workcheck.domain.checkresult.service.impl.CheckResultServiceImpl;
import com.workcheck.domain.member.entity.Member;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckResultServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CheckHistoryRepository checkHistoryRepository;

    @InjectMocks
    private CheckResultServiceImpl checkResultService;

    private Assignment assignment(Long memberId) {
        return Assignment.builder()
                .id(1L)
                .title("과제")
                .member(Member.builder().id(memberId).build())
                .build();
    }

    @Test
    void createCheckResult_allRulesPassed_returnsPassedResult() {
        given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment(1L)));
        given(checkHistoryRepository.save(any(CheckHistory.class))).willAnswer(inv -> inv.getArgument(0));

        CheckResultResponse response = checkResultService.createCheckResult(1L, new CheckResultRequest(
                1L, "20231234_홍길동.zip", FileType.ZIP, List.of(
                new RuleResultRequest(RuleType.FILE_NAME_SUFFIX, ".zip", CheckStatus.PASS, List.of()),
                new RuleResultRequest(RuleType.ZIP_REQUIRED_PATH, "src/Main.java", CheckStatus.PASS, List.of()))));

        assertThat(response.status()).isEqualTo(CheckStatus.PASS);
        assertThat(response.results()).extracting("type")
                .containsExactly(RuleType.FILE_NAME_SUFFIX, RuleType.ZIP_REQUIRED_PATH);
        assertThat(response.results()).extracting("expected")
                .containsExactly(".zip", "src/Main.java");
        verify(checkHistoryRepository).save(any(CheckHistory.class));
    }

    @Test
    void createCheckResult_aFailedRule_returnsFailedResultAndMissingItems() {
        given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment(1L)));
        given(checkHistoryRepository.save(any(CheckHistory.class))).willAnswer(inv -> inv.getArgument(0));

        CheckResultResponse response = checkResultService.createCheckResult(1L, new CheckResultRequest(
                1L, "20231234_홍길동.zip", FileType.ZIP, List.of(
                new RuleResultRequest(RuleType.FILE_NAME_SUFFIX, ".zip", CheckStatus.PASS, List.of()),
                new RuleResultRequest(RuleType.ZIP_REQUIRED_PATH, "src/main/java/Main.java",
                        CheckStatus.FAIL, List.of("src/main/java/Main.java")))));

        assertThat(response.status()).isEqualTo(CheckStatus.FAIL);
        assertThat(response.results().get(1).message())
                .isEqualTo("압축 파일에서 src/main/java/Main.java 경로를 찾지 못했어요.");
        assertThat(response.results().get(1).missingItems()).containsExactly("src/main/java/Main.java");
    }

    @Test
    void createCheckResult_reviewRule_returnsReviewResult() {
        given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment(1L)));
        given(checkHistoryRepository.save(any(CheckHistory.class))).willAnswer(inv -> inv.getArgument(0));

        CheckResultResponse response = checkResultService.createCheckResult(1L, new CheckResultRequest(
                1L, "scan.pdf", FileType.PDF, List.of(
                new RuleResultRequest(RuleType.PDF_REQUIRED_TEXT, "참고문헌", CheckStatus.REVIEW, List.of()))));

        assertThat(response.status()).isEqualTo(CheckStatus.REVIEW);
        assertThat(response.results().get(0).message())
                .isEqualTo("PDF에서 글자를 읽을 수 없어요. 스캔본이라면 직접 확인해 주세요.");
    }

    @Test
    void createCheckResult_withoutRuleResult_throwsNoApplicableRule() {
        given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment(1L)));

        assertThatThrownBy(() -> checkResultService.createCheckResult(1L, new CheckResultRequest(
                1L, "20231234_홍길동.zip", FileType.ZIP, List.of())))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NO_APPLICABLE_RULE);
        verifyNoInteractions(checkHistoryRepository);
    }

    @Test
    void createCheckResult_missingAssignment_throwsAssignmentNotFound() {
        given(assignmentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> checkResultService.createCheckResult(1L, new CheckResultRequest(
                99L, "20231234_홍길동.zip", FileType.ZIP, List.of(
                new RuleResultRequest(RuleType.FILE_NAME_SUFFIX, ".zip", CheckStatus.PASS, List.of())))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);
        verifyNoInteractions(checkHistoryRepository);
    }

    @Test
    void createCheckResult_otherMembersAssignment_throwsForbidden() {
        given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment(1L)));

        assertThatThrownBy(() -> checkResultService.createCheckResult(2L, new CheckResultRequest(
                1L, "20231234_홍길동.zip", FileType.ZIP, List.of(
                new RuleResultRequest(RuleType.FILE_NAME_SUFFIX, ".zip", CheckStatus.PASS, List.of())))))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(checkHistoryRepository);
    }
}