package com.workcheck.domain.member.service.impl;

import com.workcheck.domain.member.dto.request.LoginRequest;
import com.workcheck.domain.member.dto.request.SignupRequest;
import com.workcheck.domain.member.dto.response.MemberResponse;
import com.workcheck.domain.member.entity.Member;
import com.workcheck.domain.member.repository.MemberRepository;
import com.workcheck.domain.member.service.MemberService;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MemberServiceImpl implements MemberService {

    /**
     * 없는 이메일로 로그인해도 BCrypt 비교를 한 번 수행해 응답 시간으로 가입 여부를 알 수 없게 한다.
     * 기본 강도(10)로 만든 해시라 실제 회원 비교와 소요 시간이 같다.
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$Ubg2T/0apMovr2xiEBvMcePFJdmKd7NLo7CE.kU1JpXjaHuVLiZwS";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public MemberResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().strip())
                .build();
        try {
            return MemberResponse.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException e) {
            // 동시 가입으로 unique 제약에 걸린 경우
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest request) {
        Optional<Member> member = memberRepository.findByEmail(normalizeEmail(request.email()));
        String hash = member.map(Member::getPassword).orElse(DUMMY_PASSWORD_HASH);
        boolean matched = passwordEncoder.matches(request.password(), hash);
        if (member.isEmpty() || !matched) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }
        return MemberResponse.from(member.get());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
