package com.workcheck.domain.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.domain.member.entity.Member;
import com.workcheck.domain.member.repository.MemberRepository;
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
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    private CheckRule rule(FileType fileType, RuleType type, String value) {
        return CheckRule.builder().fileType(fileType).type(type).value(value).build();
    }

    @Test
    void save_persistsRulesAndAuditTimestamps() {
        Member member = memberRepository.save(Member.builder().email("owner@workcheck.com")
                .password("hash").name("소유자").build());
        Assignment assignment = Assignment.builder().title("과학 탐구 보고서").member(member).build();
        assignment.addRule(rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌"));
        assignmentRepository.saveAndFlush(assignment);
        entityManager.clear();

        Assignment found = assignmentRepository.findByIdWithRules(assignment.getId(), member.getId()).orElseThrow();

        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(found.getRules()).singleElement().satisfies(rule -> {
            assertThat(rule.getId()).isNotNull();
            assertThat(rule.getCreatedAt()).isNotNull();
            assertThat(rule.getAssignment().getId()).isEqualTo(found.getId());
        });
    }

    @Test
    void findAllWithRules_fetchesRulesWithoutDuplicateAssignments() {
        Member member = memberRepository.save(Member.builder().email("owner@workcheck.com")
                .password("hash").name("소유자").build());
        Assignment first = Assignment.builder().title("첫 과제").member(member).build();
        first.addRule(rule(FileType.PDF, RuleType.FILE_NAME_SUFFIX, "_보고서.pdf"));
        first.addRule(rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌"));
        assignmentRepository.saveAndFlush(first);

        Assignment second = Assignment.builder().title("두 번째 과제").member(member).build();
        second.addRule(rule(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "data.sql"));
        assignmentRepository.saveAndFlush(second);
        entityManager.clear();

        List<Assignment> found = assignmentRepository.findAllWithRules(member.getId());
        assertThat(found).allSatisfy(assignment ->
                assertThat(Hibernate.isInitialized(assignment.getRules())).isTrue());
        entityManager.clear();

        assertThat(found).extracting(Assignment::getId).containsExactly(second.getId(), first.getId());
        assertThat(found.get(1).getRules()).extracting(CheckRule::getValue)
                .containsExactly("_보고서.pdf", "참고문헌");
    }

    @Test
    void findWithRules_filtersByMemberAndHidesOtherMembersAssignments() {
        Member owner = memberRepository.save(Member.builder().email("owner@workcheck.com")
                .password("hash").name("소유자").build());
        Member other = memberRepository.save(Member.builder().email("other@workcheck.com")
                .password("hash").name("다른 회원").build());
        Assignment assignment = Assignment.builder().title("개인 과제").member(owner).build();
        assignment.addRule(rule(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌"));
        assignmentRepository.saveAndFlush(assignment);
        entityManager.clear();

        assertThat(assignmentRepository.findAllWithRules(owner.getId()))
                .extracting(Assignment::getId).containsExactly(assignment.getId());
        assertThat(assignmentRepository.findAllWithRules(other.getId())).isEmpty();
        assertThat(assignmentRepository.findByIdWithRules(assignment.getId(), owner.getId())).isPresent();
        assertThat(assignmentRepository.findByIdWithRules(assignment.getId(), other.getId())).isEmpty();
    }

}
