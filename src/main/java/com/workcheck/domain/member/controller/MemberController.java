package com.workcheck.domain.member.controller;

import com.workcheck.domain.member.dto.request.SigninRequest;
import com.workcheck.domain.member.dto.request.SignupRequest;
import com.workcheck.domain.member.dto.response.MemberResponse;
import com.workcheck.domain.member.dto.response.SigninResponse;
import com.workcheck.domain.member.service.MemberService;
import com.workcheck.global.data.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원가입 / 로그인")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<MemberResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(memberService.signup(request)));
    }

    @Operation(summary = "로그인", description = "accessToken을 발급해요. 이후 요청에 Authorization: Bearer 헤더로 보내 주세요.")
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<SigninResponse>> signin(@Valid @RequestBody SigninRequest request) {
        return ResponseEntity.ok(BaseResponse.success(memberService.signin(request)));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<MemberResponse>> me(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(BaseResponse.success(memberService.getMember(memberId)));
    }
}
