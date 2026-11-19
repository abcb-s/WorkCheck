package com.workcheck.domain.check.controller;

import com.workcheck.domain.check.dto.response.CheckResponse;
import com.workcheck.domain.check.dto.response.CheckSummaryResponse;
import com.workcheck.domain.check.service.CheckService;
import com.workcheck.global.data.BaseResponse;
import com.workcheck.global.data.CheckStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Check", description = "제출 파일 검사")
@RestController
@RequestMapping("/api/assignments/{assignmentId}/checks")
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;

    @Operation(summary = "제출 파일 검사")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<CheckResponse>> check(
            @AuthenticationPrincipal Long memberId, @PathVariable Long assignmentId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(checkService.check(memberId, assignmentId, file)));
    }

    @Operation(summary = "검사 기록 목록 조회")
    @GetMapping
    public ResponseEntity<BaseResponse<List<CheckSummaryResponse>>> getChecks(
            @AuthenticationPrincipal Long memberId, @PathVariable Long assignmentId,
            @RequestParam(required = false) CheckStatus status) {
        return ResponseEntity.ok(BaseResponse.success(checkService.getChecks(memberId, assignmentId, status)));
    }

    @Operation(summary = "검사 기록 상세 조회")
    @GetMapping("/{checkId}")
    public ResponseEntity<BaseResponse<CheckResponse>> getCheck(
            @AuthenticationPrincipal Long memberId, @PathVariable Long assignmentId, @PathVariable Long checkId) {
        return ResponseEntity.ok(BaseResponse.success(checkService.getCheck(memberId, assignmentId, checkId)));
    }
}
