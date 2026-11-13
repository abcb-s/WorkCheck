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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@ContextConfiguration(classes = AssignmentRepositoryTest.JpaTestConfig.class)
class AssignmentRepositoryTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableJpaAuditing
    @EntityScan(basePackageClasses = Assignment.class)
    @EnableJpaRepositories(basePackageClasses = AssignmentRepository.class)
    static class JpaTestConfig {
    }

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CheckRuleRepository checkRuleRepository;

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

        assertThat(found).extracting(Assignment::getId).containsExactly(second.getId(), first.getId());
        assertThat(found.get(1).getRules()).extracting(CheckRule::getValue)
                .containsExactly("_보고서.pdf", "참고문헌");
    }

    @Test
    void findRules_filtersByAssignmentAndFileType() {
        Assignment assignment = Assignment.builder().title("프로젝트").build();
        assignment.addRule(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql"));
        assignment.addRule(rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌"));
        assignmentRepository.saveAndFlush(assignment);

        Assignment other = Assignment.builder().title("다른 프로젝트").build();
        other.addRule(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "README.md"));
        assignmentRepository.saveAndFlush(other);
        entityManager.clear();

        List<CheckRule> rules = checkRuleRepository.findByAssignment_IdAndFileType(
                assignment.getId(), FileType.ZIP);

        assertThat(rules).singleElement().satisfies(rule -> {
            assertThat(rule.getValue()).isEqualTo("data.sql");
            assertThat(rule.getAssignment().getId()).isEqualTo(assignment.getId());
        });
        assertThat(checkRuleRepository.findByAssignment_IdAndFileType(0L, FileType.PDF)).isEmpty();
    }
}
