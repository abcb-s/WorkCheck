package com.workcheck.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workcheck.domain.member.entity.Member;
import com.workcheck.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private Member member(String email) {
        return Member.builder().email(email).password("hash").name("학생").build();
    }

    @Test
    void save_setsAuditTimestamps_andFindsByEmail() {
        memberRepository.saveAndFlush(member("a@b.com"));

        Member found = memberRepository.findByEmail("a@b.com").orElseThrow();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(memberRepository.existsByEmail("a@b.com")).isTrue();
        assertThat(memberRepository.existsByEmail("none@b.com")).isFalse();
    }

    @Test
    void duplicateEmail_violatesUniqueConstraint() {
        memberRepository.saveAndFlush(member("a@b.com"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(member("a@b.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
