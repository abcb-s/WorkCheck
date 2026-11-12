package com.workcheck.domain.member.controller;

import com.workcheck.domain.member.dto.request.LoginRequest;
import com.workcheck.domain.member.dto.request.SignupRequest;
import com.workcheck.domain.member.dto.response.MemberResponse;
import com.workcheck.domain.member.service.MemberService;
import com.workcheck.global.data.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원가입 / 세션 로그인")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "CSRF 토큰 발급", description = "XSRF-TOKEN 쿠키를 내려줘요. 이후 변경 요청에 같은 값을 X-XSRF-TOKEN 헤더로 보내 주세요.")
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원가입")
    @PostMapping
    public ResponseEntity<BaseResponse<MemberResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(memberService.signup(request)));
    }

    @Operation(summary = "로그인", description = "성공하면 세션 쿠키(JSESSIONID)가 발급돼요.")
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<MemberResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(BaseResponse.success(memberService.login(request)));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<MemberResponse>> me(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(BaseResponse.success(memberService.getMember(memberId)));
    }
}
