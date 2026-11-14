package com.workcheck.domain.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.workcheck.domain.assignment.dto.request.AssignmentRequest;
import com.workcheck.domain.assignment.dto.request.CheckRuleRequest;
import com.workcheck.domain.assignment.dto.response.AssignmentResponse;
import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.assignment.service.impl.AssignmentServiceImpl;
import com.workcheck.domain.member.entity.Member;
import com.workcheck.domain.member.repository.MemberRepository;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    private static final Member MEMBER = Member.builder().id(1L).email("owner@workcheck.com").name("소유자").build();

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Test
    void createAssignment_stripsValuesAndConnectsRules() {
        AssignmentRequest request = new AssignmentRequest("  과학 탐구 보고서  ", List.of(
                new CheckRuleRequest(FileType.PDF, RuleType.FILE_NAME_SUFFIX, "  _보고서.pdf  "),
                new CheckRuleRequest(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "  참고 문헌  "),
                new CheckRuleRequest(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "  data.sql  ")
        ));
        given(memberRepository.findById(1L)).willReturn(Optional.of(MEMBER));
        given(assignmentRepository.save(any(Assignment.class))).willAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            assertThat(assignment.getMember()).isSameAs(MEMBER);
            assertThat(assignment.getRules()).hasSize(3);
            assertThat(assignment.getRules()).allSatisfy(rule ->
                    assertThat(rule.getAssignment()).isSameAs(assignment));
            return assignment;
        });

        AssignmentResponse response = assignmentService.createAssignment(1L, request);

        assertThat(response.title()).isEqualTo("과학 탐구 보고서");
        assertThat(response.rules()).extracting("value")
                .containsExactly("_보고서.pdf", "참고 문헌", "data.sql");
    }

    @ParameterizedTest
    @CsvSource({"ZIP, PDF_REQUIRED_TEXT", "PDF, ZIP_REQUIRED_PATH"})
    void createAssignment_wrongRuleCombination_doesNotSave(FileType fileType, RuleType type) {
        AssignmentRequest request = new AssignmentRequest("과제", List.of(
                new CheckRuleRequest(FileType.ZIP, RuleType.FILE_NAME_SUFFIX, ".zip"),
                new CheckRuleRequest(fileType, type, "필수값")
        ));

        assertThatThrownBy(() -> assignmentService.createAssignment(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_RULE_COMBINATION);

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void createAssignment_missingMember_doesNotSave() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());
        AssignmentRequest request = new AssignmentRequest("과제", List.of(
                new CheckRuleRequest(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌")
        ));

        assertThatThrownBy(() -> assignmentService.createAssignment(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void getAssignments_returnsEmptyListWhenNoneExist() {
        given(assignmentRepository.findAllWithRules(1L)).willReturn(List.of());

        assertThat(assignmentService.getAssignments(1L)).isEmpty();
    }

    @Test
    void getAssignment_mapsRulesToResponse() {
        Assignment assignment = Assignment.builder().id(1L).title("자바 프로젝트").member(MEMBER).build();
        assignment.addRule(CheckRule.builder().id(10L).fileType(FileType.ZIP)
                .type(RuleType.ZIP_REQUIRED_PATH).value("data.sql").build());
        given(assignmentRepository.findByIdWithRules(1L, 1L)).willReturn(Optional.of(assignment));

        AssignmentResponse response = assignmentService.getAssignment(1L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.rules()).singleElement().satisfies(rule -> {
            assertThat(rule.id()).isEqualTo(10L);
            assertThat(rule.value()).isEqualTo("data.sql");
        });
    }

    @Test
    void getAssignment_notFound_throws() {
        given(assignmentRepository.findByIdWithRules(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.getAssignment(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);
    }

    @ParameterizedTest
    @CsvSource({"ZIP, PDF_REQUIRED_TEXT", "PDF, ZIP_REQUIRED_PATH"})
    void updateAssignment_wrongRuleCombination_keepsTitleAndRules(FileType fileType, RuleType type) {
        Assignment assignment = Assignment.builder().id(1L).title("기존 과제").member(MEMBER).build();
        CheckRule originalRule = CheckRule.builder().id(10L).fileType(FileType.PDF)
                .type(RuleType.PDF_REQUIRED_TEXT).value("참고문헌").build();
        assignment.addRule(originalRule);
        given(assignmentRepository.findByIdWithRules(1L, 1L)).willReturn(Optional.of(assignment));
        AssignmentRequest request = new AssignmentRequest("수정한 과제", List.of(
                new CheckRuleRequest(FileType.ZIP, RuleType.FILE_NAME_SUFFIX, ".zip"),
                new CheckRuleRequest(fileType, type, "필수값")
        ));

        assertThatThrownBy(() -> assignmentService.updateAssignment(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_RULE_COMBINATION);

        assertThat(assignment.getTitle()).isEqualTo("기존 과제");
        assertThat(assignment.getRules()).containsExactly(originalRule);
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({
            "ZIP, FILE_NAME_SUFFIX, _프로젝트.zip",
            "PDF, FILE_NAME_SUFFIX, _보고서.pdf",
            "ZIP, ZIP_REQUIRED_PATH, src/main/resources/data.sql",
            "PDF, PDF_REQUIRED_TEXT, 참고 문헌"
    })
    void updateAssignment_stripsValueAndReplacesRules(FileType fileType, RuleType type, String value) {
        Assignment assignment = Assignment.builder().id(1L).title("기존 과제").member(MEMBER).build();
        CheckRule originalRule = CheckRule.builder().id(10L).fileType(FileType.PDF)
                .type(RuleType.PDF_REQUIRED_TEXT).value("참고문헌").build();
        assignment.addRule(originalRule);
        List<CheckRule> originalRules = assignment.getRules();
        given(assignmentRepository.findByIdWithRules(1L, 1L)).willReturn(Optional.of(assignment));
        given(assignmentRepository.saveAndFlush(assignment)).willReturn(assignment);
        AssignmentRequest request = new AssignmentRequest("  수정한 과제  ", List.of(
                new CheckRuleRequest(fileType, type, "  " + value + "  ")
        ));

        AssignmentResponse response = assignmentService.updateAssignment(1L, 1L, request);

        assertThat(response.title()).isEqualTo("수정한 과제");
        assertThat(response.rules()).extracting("value").containsExactly(value);
        assertThat(assignment.getRules()).isSameAs(originalRules).doesNotContain(originalRule);
        assertThat(assignment.getRules()).singleElement().satisfies(rule -> {
            assertThat(rule.getFileType()).isEqualTo(fileType);
            assertThat(rule.getType()).isEqualTo(type);
            assertThat(rule.getAssignment()).isSameAs(assignment);
        });
    }

    @Test
    void updateAssignment_notFound_throws() {
        given(assignmentRepository.findByIdWithRules(99L, 1L)).willReturn(Optional.empty());
        AssignmentRequest request = new AssignmentRequest("과제", List.of(
                new CheckRuleRequest(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌")
        ));

        assertThatThrownBy(() -> assignmentService.updateAssignment(1L, 99L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);

        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteAssignment_notFound_throws() {
        given(assignmentRepository.findByIdWithRules(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.deleteAssignment(1L, 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);

        verify(assignmentRepository, never()).delete(any(Assignment.class));
    }

    @Test
    void accessOtherMembersAssignment_returnsNotFoundWithoutSavingOrDeleting() {
        given(assignmentRepository.findByIdWithRules(1L, 2L)).willReturn(Optional.empty());
        AssignmentRequest request = new AssignmentRequest("수정 시도", List.of(
                new CheckRuleRequest(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "결론")
        ));

        assertThatThrownBy(() -> assignmentService.getAssignment(2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);
        assertThatThrownBy(() -> assignmentService.updateAssignment(2L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);
        assertThatThrownBy(() -> assignmentService.deleteAssignment(2L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);

        verify(assignmentRepository, never()).saveAndFlush(any());
        verify(assignmentRepository, never()).delete(any(Assignment.class));
    }
}
