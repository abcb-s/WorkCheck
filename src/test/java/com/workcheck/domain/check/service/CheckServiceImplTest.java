package com.workcheck.domain.check.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.check.dto.response.CheckResponse;
import com.workcheck.domain.check.dto.response.CheckSummaryResponse;
import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.domain.check.entity.CheckResult;
import com.workcheck.domain.check.repository.CheckHistoryRepository;
import com.workcheck.domain.check.service.impl.CheckServiceImpl;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CheckServiceImplTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CheckHistoryRepository checkHistoryRepository;

    @InjectMocks
    private CheckServiceImpl checkService;

    private static CheckRule rule(FileType fileType, RuleType type, String value) {
        return CheckRule.builder().fileType(fileType).type(type).value(value).build();
    }

    private static Assignment assignment(CheckRule... rules) {
        Assignment assignment = Assignment.builder().title("과제").build();
        assignment.replaceRules(List.of(rules));
        return assignment;
    }

    private static byte[] zip(String... entryNames) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String name : entryNames) {
                zip.putNextEntry(new ZipEntry(name));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static byte[] pdf(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (text != null) {
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            document.save(out);
        }
        return out.toByteArray();
    }

    private void givenAssignment(Assignment assignment) {
        given(assignmentRepository.findByIdWithRules(1L, 1L)).willReturn(Optional.of(assignment));
    }

    private void givenSaveReturnsArgument() {
        given(checkHistoryRepository.save(any(CheckHistory.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    private CheckHistory savedHistory() {
        ArgumentCaptor<CheckHistory> captor = ArgumentCaptor.forClass(CheckHistory.class);
        verify(checkHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void check_zipMissingRequiredFile_savesFailHistory() throws IOException {
        Assignment assignment = assignment(
                rule(FileType.ZIP, RuleType.FILE_NAME_SUFFIX, "_프로젝트.zip"),
                rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "src/main/resources/data.sql"));
        givenAssignment(assignment);
        givenSaveReturnsArgument();
        MockMultipartFile file = new MockMultipartFile("file", "홍길동_프로젝트.zip", "application/zip",
                zip("src/main/java/App.java"));

        CheckResponse response = checkService.check(1L, 1L, file);

        assertThat(response.status()).isEqualTo(CheckStatus.FAIL);
        assertThat(response.fileName()).isEqualTo("홍길동_프로젝트.zip");
        assertThat(response.results()).extracting("status").containsExactly(CheckStatus.PASS, CheckStatus.FAIL);
        assertThat(response.results().get(1).message())
                .isEqualTo("압축 파일에서 src/main/resources/data.sql 경로를 찾지 못했어요.");
        assertThat(response.results().get(0).missingItems()).isEmpty();
        assertThat(response.results().get(1).missingItems()).containsExactly("src/main/resources/data.sql");

        CheckHistory saved = savedHistory();
        assertThat(saved.getAssignment()).isSameAs(assignment);
        assertThat(saved.getStatus()).isEqualTo(CheckStatus.FAIL);
        assertThat(saved.getResults()).allSatisfy(result -> assertThat(result.getCheckHistory()).isSameAs(saved));
    }

    @Test
    void check_githubZipWithRootFolder_passes() throws IOException {
        givenAssignment(assignment(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "src/main/resources/data.sql")));
        givenSaveReturnsArgument();
        MockMultipartFile file = new MockMultipartFile("file", "project-main.zip", "application/zip",
                zip("project-main/README.md", "project-main/src/main/resources/data.sql"));

        CheckResponse response = checkService.check(1L, 1L, file);

        assertThat(response.status()).isEqualTo(CheckStatus.PASS);
    }

    @Test
    void check_pdfContainingRequiredText_passes() throws IOException {
        givenAssignment(assignment(
                rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "References"),
                rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql")));
        givenSaveReturnsArgument();
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf",
                pdf("Introduction and References"));

        CheckResponse response = checkService.check(1L, 1L, file);

        assertThat(response.status()).isEqualTo(CheckStatus.PASS);
        assertThat(response.results()).singleElement().satisfies(result -> {
            assertThat(result.type()).isEqualTo(RuleType.PDF_REQUIRED_TEXT);
            assertThat(result.message()).isEqualTo("PDF에서 'References' 문구를 찾았어요.");
        });
    }

    @Test
    void check_pdfMissingRequiredText_fails() throws IOException {
        givenAssignment(assignment(rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "References")));
        givenSaveReturnsArgument();
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", pdf("Introduction"));

        CheckResponse response = checkService.check(1L, 1L, file);

        assertThat(response.status()).isEqualTo(CheckStatus.FAIL);
        assertThat(response.results()).singleElement()
                .satisfies(result -> assertThat(result.missingItems()).containsExactly("References"));
    }

    @Test
    void check_pdfWithoutText_needsReview() throws IOException {
        givenAssignment(assignment(
                rule(FileType.PDF, RuleType.FILE_NAME_SUFFIX, ".pdf"),
                rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "References")));
        givenSaveReturnsArgument();
        MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", pdf(null));

        CheckResponse response = checkService.check(1L, 1L, file);

        assertThat(response.status()).isEqualTo(CheckStatus.REVIEW);
        assertThat(response.results()).extracting("status").containsExactly(CheckStatus.PASS, CheckStatus.REVIEW);
        assertThat(response.results()).allSatisfy(result -> assertThat(result.missingItems()).isEmpty());
    }

    @Test
    void check_corruptedZip_throwsInvalidFile() {
        givenAssignment(assignment(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql")));
        byte[] corrupted = {0x50, 0x4B, 0x03, 0x04, 0x01, 0x02, 0x03};
        MockMultipartFile file = new MockMultipartFile("file", "project.zip", "application/zip", corrupted);

        assertThatThrownBy(() -> checkService.check(1L, 1L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
        verify(checkHistoryRepository, never()).save(any());
    }

    @Test
    void check_extensionAndContentMismatch_throwsInvalidFile() throws IOException {
        givenAssignment(assignment(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql")));
        MockMultipartFile file = new MockMultipartFile("file", "project.zip", "application/zip", pdf("text"));

        assertThatThrownBy(() -> checkService.check(1L, 1L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
        verify(checkHistoryRepository, never()).save(any());
    }

    @Test
    void check_unsupportedExtension_throwsUnsupportedFileType() {
        givenAssignment(assignment(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql")));
        MockMultipartFile file = new MockMultipartFile("file", "memo.txt", "text/plain",
                "memo".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> checkService.check(1L, 1L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void check_emptyFile_throwsEmptyFile() {
        givenAssignment(assignment(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql")));
        MockMultipartFile file = new MockMultipartFile("file", "project.zip", "application/zip", new byte[0]);

        assertThatThrownBy(() -> checkService.check(1L, 1L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMPTY_FILE);
    }

    @Test
    void check_noRuleForFileType_throwsNoApplicableRule() throws IOException {
        givenAssignment(assignment(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql")));
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", pdf("text"));

        assertThatThrownBy(() -> checkService.check(1L, 1L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NO_APPLICABLE_RULE);
        verify(checkHistoryRepository, never()).save(any());
    }

    @Test
    void check_otherMembersAssignment_throwsAssignmentNotFound() throws IOException {
        given(assignmentRepository.findByIdWithRules(1L, 2L)).willReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "project.zip", "application/zip", zip("data.sql"));

        assertThatThrownBy(() -> checkService.check(2L, 1L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);
        verifyNoInteractions(checkHistoryRepository);
    }

    private static CheckHistory history(String fileName, CheckStatus status) {
        Assignment assignment = Assignment.builder().id(1L).title("자바 프로젝트").build();
        return CheckHistory.builder().assignment(assignment).fileName(fileName).status(status).build();
    }

    @Test
    void getMyChecks_withoutStatus_returnsAllWithAssignment() {
        given(checkHistoryRepository.findAllByMemberId(1L)).willReturn(List.of(
                history("second.zip", CheckStatus.PASS), history("first.zip", CheckStatus.FAIL)));

        List<CheckSummaryResponse> responses = checkService.getMyChecks(1L, null);

        assertThat(responses).extracting("fileName").containsExactly("second.zip", "first.zip");
        assertThat(responses).allSatisfy(response -> {
            assertThat(response.assignmentId()).isEqualTo(1L);
            assertThat(response.assignmentTitle()).isEqualTo("자바 프로젝트");
        });
        verify(checkHistoryRepository, never()).findAllByMemberIdAndStatus(any(), any());
    }

    @Test
    void getMyChecks_withStatus_usesStatusFilter() {
        given(checkHistoryRepository.findAllByMemberIdAndStatus(1L, CheckStatus.FAIL))
                .willReturn(List.of(history("project.zip", CheckStatus.FAIL)));

        List<CheckSummaryResponse> responses = checkService.getMyChecks(1L, CheckStatus.FAIL);

        assertThat(responses).extracting("status").containsExactly(CheckStatus.FAIL);
        verify(checkHistoryRepository, never()).findAllByMemberId(any());
    }

    @Test
    void getChecks_withStatus_usesStatusFilter() {
        CheckHistory history = history("project.zip", CheckStatus.FAIL);
        given(assignmentRepository.existsByIdAndMember_Id(1L, 1L)).willReturn(true);
        given(checkHistoryRepository.findByAssignment_IdAndStatusOrderByCreatedAtDescIdDesc(1L, CheckStatus.FAIL))
                .willReturn(List.of(history));

        List<CheckSummaryResponse> responses = checkService.getChecks(1L, 1L, CheckStatus.FAIL);

        assertThat(responses).extracting("fileName").containsExactly("project.zip");
        verify(checkHistoryRepository, never()).findByAssignment_IdOrderByCreatedAtDescIdDesc(any());
    }

    @Test
    void getChecks_withoutStatus_returnsAll() {
        given(assignmentRepository.existsByIdAndMember_Id(1L, 1L)).willReturn(true);
        given(checkHistoryRepository.findByAssignment_IdOrderByCreatedAtDescIdDesc(1L)).willReturn(List.of(
                history("second.zip", CheckStatus.PASS), history("first.zip", CheckStatus.FAIL)));

        List<CheckSummaryResponse> responses = checkService.getChecks(1L, 1L, null);

        assertThat(responses).extracting("fileName").containsExactly("second.zip", "first.zip");
    }

    @Test
    void getChecks_otherMembersAssignment_throwsAssignmentNotFound() {
        given(assignmentRepository.existsByIdAndMember_Id(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> checkService.getChecks(2L, 1L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);
        verifyNoInteractions(checkHistoryRepository);
    }

    @Test
    void getCheck_returnsResults() {
        CheckHistory history = CheckHistory.builder().fileName("project.zip").status(CheckStatus.FAIL).build();
        history.addResult(CheckResult.builder().type(RuleType.ZIP_REQUIRED_PATH).expected("data.sql")
                .status(CheckStatus.FAIL).message("압축 파일에서 data.sql 경로를 찾지 못했어요.").build());
        given(assignmentRepository.existsByIdAndMember_Id(1L, 1L)).willReturn(true);
        given(checkHistoryRepository.findByIdWithResults(5L, 1L)).willReturn(Optional.of(history));

        CheckResponse response = checkService.getCheck(1L, 1L, 5L);

        assertThat(response.results()).singleElement()
                .satisfies(result -> assertThat(result.expected()).isEqualTo("data.sql"));
    }

    @Test
    void getCheck_checkOfOtherAssignment_throwsCheckNotFound() {
        given(assignmentRepository.existsByIdAndMember_Id(1L, 1L)).willReturn(true);
        given(checkHistoryRepository.findByIdWithResults(5L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> checkService.getCheck(1L, 1L, 5L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECK_NOT_FOUND);
    }

    @Test
    void getCheck_otherMembersAssignment_throwsAssignmentNotFound() {
        given(assignmentRepository.existsByIdAndMember_Id(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> checkService.getCheck(2L, 1L, 5L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ASSIGNMENT_NOT_FOUND);
        verifyNoInteractions(checkHistoryRepository);
    }
}
