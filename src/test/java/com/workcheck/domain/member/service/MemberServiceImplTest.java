package com.workcheck.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.workcheck.domain.member.dto.request.SigninRequest;
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
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.signup(new SignupRequest("A@B.com", "password1", " 학생 "));

        assertThat(response.email()).isEqualTo("a@b.com");
        assertThat(response.name()).isEqualTo("학생");
        verify(memberRepository).save(argThat(
                m -> !m.getPassword().equals("password1") && passwordEncoder.matches("password1", m.getPassword())));
    }

    @Test
    void signup_duplicateEmail_throws() {
        given(memberRepository.existsByEmail("a@b.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(new SignupRequest("a@b.com", "password1", "학생")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void signin_success() {
        Member member = Member.builder().id(1L).email("a@b.com")
                .password(passwordEncoder.encode("password1")).name("학생").build();
        given(memberRepository.findByEmail("a@b.com")).willReturn(Optional.of(member));

        assertThat(memberService.signin(new SigninRequest("A@b.com", "password1")).id()).isEqualTo(1L);
        verify(sessionAuthenticator).signin(1L);
    }

    @Test
    void signin_wrongPassword_throws() {
        Member member = Member.builder().id(1L).email("a@b.com")
                .password(passwordEncoder.encode("password1")).name("학생").build();
        given(memberRepository.findByEmail("a@b.com")).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.signin(new SigninRequest("a@b.com", "wrong")))
                .extracting("errorCode").isEqualTo(ErrorCode.SIGNIN_FAILED);
        verify(sessionAuthenticator, never()).signin(any());
    }

    @Test
    void signin_unknownEmail_throwsSameError() {
        given(memberRepository.findByEmail("x@b.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.signin(new SigninRequest("x@b.com", "password1")))
                .extracting("errorCode").isEqualTo(ErrorCode.SIGNIN_FAILED);
        verify(sessionAuthenticator, never()).signin(any());
    }

    @Test
    void getMember_notFound_throws() {
        given(memberRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(9L))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
