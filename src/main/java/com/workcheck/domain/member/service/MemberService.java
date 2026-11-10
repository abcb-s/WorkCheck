package com.workcheck.domain.member.service;

import com.workcheck.domain.member.dto.request.LoginRequest;
import com.workcheck.domain.member.dto.request.SignupRequest;
import com.workcheck.domain.member.dto.response.MemberResponse;

public interface MemberService {

    MemberResponse signup(SignupRequest request);

    MemberResponse login(LoginRequest request);

    MemberResponse getMember(Long memberId);
}
