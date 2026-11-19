package com.workcheck.domain.member.service;

import com.workcheck.domain.member.dto.request.SigninRequest;
import com.workcheck.domain.member.dto.request.SignupRequest;
import com.workcheck.domain.member.dto.response.MemberResponse;
import com.workcheck.domain.member.dto.response.SigninResponse;

public interface MemberService {

    MemberResponse signup(SignupRequest request);

    SigninResponse signin(SigninRequest request);

    MemberResponse getMember(Long memberId);
}
