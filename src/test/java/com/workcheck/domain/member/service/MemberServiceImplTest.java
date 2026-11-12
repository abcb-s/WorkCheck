package com.workcheck.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.workcheck.domain.member.dto.request.LoginRequest;
import com.workcheck.domain.member.dto.request.SignupRequest;
import com.workcheck.domain.member.dto.response.MemberResponse;
import com.workcheck.domain.member.entity.Member;
import com.workcheck.domain.member.repository.MemberRepository;
import com.workcheck.domain.member.service.impl.MemberServiceImpl;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import com.workcheck.global.security.SessionAuthenticator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private SessionAuthenticator sessionAuthenticator;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void signup_normalizesEmailAndEncodesPassword() {
        given(memberRepository.existsByEmail("a@b.com")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.signup(new SignupRequest("  A@B.com ", "password1", " 학생 "));

        assertThat(response.email()).isEqualTo("a@b.com");
        assertThat(response.name()).isEqualTo("학생");
        verify(memberRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                m -> !m.getPassword().equals("password1") && passwordEncoder.matches("password1", m.getPassword())));
    }

    @Test
    void signup_duplicateEmail_throws() {
        given(memberRepository.existsByEmail("a@b.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(new SignupRequest("a@b.com", "password1", "학생")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void signup_uniqueConstraintRace_throwsDuplicate() {
        given(memberRepository.existsByEmail("a@b.com")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class))).willThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> memberService.signup(new SignupRequest("a@b.com", "password1", "학생")))
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void login_success() {
        Member member = Member.builder().id(1L).email("a@b.com")
                .password(passwordEncoder.encode("password1")).name("학생").build();
        given(memberRepository.findByEmail("a@b.com")).willReturn(Optional.of(member));

        assertThat(memberService.login(new LoginRequest("A@b.com", "password1")).id()).isEqualTo(1L);
        verify(sessionAuthenticator).login(1L);
    }

    @Test
    void login_wrongPassword_throws() {
        Member member = Member.builder().id(1L).email("a@b.com")
                .password(passwordEncoder.encode("password1")).name("학생").build();
        given(memberRepository.findByEmail("a@b.com")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.login(new LoginRequest("a@b.com", "wrong")))
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);
        verify(sessionAuthenticator, never()).login(any());
    }

    @Test
    void login_unknownEmail_throwsSameError() {
        given(memberRepository.findByEmail("x@b.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(new LoginRequest("x@b.com", "password1")))
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_FAILED);
        verify(passwordEncoder).matches(org.mockito.ArgumentMatchers.eq("password1"), any());
    }

    @Test
    void getMember_notFound_throws() {
        given(memberRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(9L))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
