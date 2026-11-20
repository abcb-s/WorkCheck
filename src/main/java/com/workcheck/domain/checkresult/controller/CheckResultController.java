package com.workcheck.domain.checkresult.controller;

import com.workcheck.domain.checkresult.dto.request.CheckResultRequest;
import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;
import com.workcheck.domain.checkresult.service.CheckResultService;
import com.workcheck.global.data.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Check Result", description = "제출물 검사 결과")
@RestController
@RequestMapping("/api/check-results")
@RequiredArgsConstructor
public class CheckResultController {

    private final CheckResultService checkResultService;

    @Operation(summary = "검사 결과 생성")
    @PostMapping
    public ResponseEntity<BaseResponse<CheckResultResponse>> createCheckResult(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CheckResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(checkResultService.createCheckResult(memberId, request)));
    }
}