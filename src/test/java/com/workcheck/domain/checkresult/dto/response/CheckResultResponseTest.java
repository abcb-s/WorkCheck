package com.workcheck.domain.checkresult.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workcheck.domain.checkresult.type.CheckRuleType;
import com.workcheck.global.data.FileType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckResultResponseTest {

    @Test
    void preservesRuleOutcomesAndDoesNotExposeMutableLists() {
        List<String> missingItems = new ArrayList<>(List.of("src/main/java/Main.java"));
        RuleCheckResultResponse ruleResult = new RuleCheckResultResponse(
                10L, CheckRuleType.ZIP_REQUIRED_FILE, false, missingItems);
        List<RuleCheckResultResponse> results = new ArrayList<>(List.of(ruleResult));

        CheckResultResponse response = new CheckResultResponse(
                1L, "20231234_홍길동.zip", FileType.ZIP, false, results);

        missingItems.clear();
        results.clear();

        assertThat(response.assignmentId()).isEqualTo(1L);
        assertThat(response.fileName()).isEqualTo("20231234_홍길동.zip");
        assertThat(response.fileType()).isEqualTo(FileType.ZIP);
        assertThat(response.passed()).isFalse();
        assertThat(response.results()).singleElement().satisfies(result -> {
            assertThat(result.ruleId()).isEqualTo(10L);
            assertThat(result.ruleType()).isEqualTo(CheckRuleType.ZIP_REQUIRED_FILE);
            assertThat(result.passed()).isFalse();
            assertThat(result.missingItems()).containsExactly("src/main/java/Main.java");
        });
        assertThatThrownBy(() -> response.results().add(ruleResult))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ruleResult.missingItems().add("README.md"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
