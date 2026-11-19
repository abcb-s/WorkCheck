package com.workcheck.domain.check.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.domain.check.entity.CheckResult;
import com.workcheck.domain.member.entity.Member;
import com.workcheck.domain.member.repository.MemberRepository;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class CheckHistoryRepositoryTest {

    @Autowired
    private CheckHistoryRepository checkHistoryRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder().email("owner@workcheck.com")
                .password("hash").name("소유자").build());
        assignment = Assignment.builder().title("자바 프로젝트").member(member).build();
        assignment.addRule(CheckRule.builder().fileType(FileType.ZIP)
                .type(RuleType.ZIP_REQUIRED_PATH).value("data.sql").build());
        assignmentRepository.saveAndFlush(assignment);
    }

    private CheckHistory saveHistory(Assignment target, String fileName, CheckStatus status) {
        CheckHistory history = CheckHistory.builder().assignment(target).fileName(fileName).status(status).build();
        history.addResult(CheckResult.builder().type(RuleType.ZIP_REQUIRED_PATH).expected("data.sql")
                .status(status).message("결과").build());
        return checkHistoryRepository.saveAndFlush(history);
    }

    @Test
    void findByAssignment_returnsNewestFirst() {
        CheckHistory first = saveHistory(assignment, "first.zip", CheckStatus.FAIL);
        CheckHistory second = saveHistory(assignment, "second.zip", CheckStatus.PASS);
        entityManager.clear();

        List<CheckHistory> found = checkHistoryRepository.findByAssignment_IdOrderByCreatedAtDescIdDesc(assignment.getId());

        assertThat(found).extracting(CheckHistory::getId).containsExactly(second.getId(), first.getId());
    }

    @Test
    void findByAssignmentAndStatus_returnsOnlyMatchingStatus() {
        saveHistory(assignment, "fail.zip", CheckStatus.FAIL);
        saveHistory(assignment, "pass.zip", CheckStatus.PASS);
        entityManager.clear();

        List<CheckHistory> found = checkHistoryRepository
                .findByAssignment_IdAndStatusOrderByCreatedAtDescIdDesc(assignment.getId(), CheckStatus.FAIL);

        assertThat(found).extracting(CheckHistory::getFileName).containsExactly("fail.zip");
    }

    @Test
    void findByIdWithResults_fetchesResults() {
        CheckHistory history = saveHistory(assignment, "project.zip", CheckStatus.FAIL);
        entityManager.clear();

        CheckHistory found = checkHistoryRepository.findByIdWithResults(history.getId(), assignment.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getResults())).isTrue();
        assertThat(found.getResults()).singleElement()
                .satisfies(result -> assertThat(result.getExpected()).isEqualTo("data.sql"));
    }

    @Test
    void findByIdWithResults_otherAssignment_returnsEmpty() {
        Assignment other = assignmentRepository.saveAndFlush(
                Assignment.builder().title("다른 과제").member(member).build());
        CheckHistory history = saveHistory(assignment, "project.zip", CheckStatus.FAIL);
        entityManager.clear();

        assertThat(checkHistoryRepository.findByIdWithResults(history.getId(), other.getId())).isEmpty();
    }

    @Test
    void deleteAssignment_deletesHistoriesAndResults() {
        CheckHistory history = saveHistory(assignment, "project.zip", CheckStatus.FAIL);
        entityManager.clear();

        assignmentRepository.delete(assignmentRepository.findById(assignment.getId()).orElseThrow());
        assignmentRepository.flush();

        assertThat(checkHistoryRepository.findById(history.getId())).isEmpty();
        assertThat(entityManager.createQuery(
                        "select count(r) from CheckResult r where r.checkHistory.id = :historyId", Long.class)
                .setParameter("historyId", history.getId())
                .getSingleResult()).isZero();
    }
}
