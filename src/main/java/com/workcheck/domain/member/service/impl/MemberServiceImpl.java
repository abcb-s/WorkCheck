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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MemberServiceImpl implements MemberService {

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
        Member member = memberRepository.findByEmail(normalizeEmail(request.email()))
                .filter(m -> passwordEncoder.matches(request.password(), m.getPassword()))
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));
        return MemberResponse.from(member);
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
