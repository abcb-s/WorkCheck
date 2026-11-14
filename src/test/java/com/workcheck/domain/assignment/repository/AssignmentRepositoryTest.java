package com.workcheck.domain.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AssignmentRepositoryTest {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private EntityManager entityManager;

    private CheckRule rule(FileType fileType, RuleType type, String value) {
        return CheckRule.builder().fileType(fileType).type(type).value(value).build();
    }

    @Test
    void save_persistsRulesAndAuditTimestamps() {
        Assignment assignment = Assignment.builder().title("과학 탐구 보고서").build();
        assignment.addRule(rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌"));
        assignmentRepository.saveAndFlush(assignment);
        entityManager.clear();

        Assignment found = assignmentRepository.findByIdWithRules(assignment.getId()).orElseThrow();

        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getRules()).singleElement().satisfies(rule -> {
            assertThat(rule.getId()).isNotNull();
            assertThat(rule.getCreatedAt()).isNotNull();
            assertThat(rule.getAssignment().getId()).isEqualTo(found.getId());
        });
    }

    @Test
    void findAllWithRules_fetchesRulesWithoutDuplicateAssignments() {
        Assignment first = Assignment.builder().title("첫 과제").build();
        first.addRule(rule(FileType.PDF, RuleType.FILE_NAME_SUFFIX, "_보고서.pdf"));
        first.addRule(rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌"));
        assignmentRepository.saveAndFlush(first);

        Assignment second = Assignment.builder().title("두 번째 과제").build();
        second.addRule(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql"));
        assignmentRepository.saveAndFlush(second);
        entityManager.clear();

        List<Assignment> found = assignmentRepository.findAllWithRules();
        assertThat(found).allSatisfy(assignment ->
                assertThat(Hibernate.isInitialized(assignment.getRules())).isTrue());
        entityManager.clear();

        assertThat(found).extracting(Assignment::getId).startsWith(second.getId(), first.getId()).doesNotHaveDuplicates();
        assertThat(found.get(1).getRules()).extracting(CheckRule::getValue)
                .containsExactly("_보고서.pdf", "참고문헌");
    }

}
