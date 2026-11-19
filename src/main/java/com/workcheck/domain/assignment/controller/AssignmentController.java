package com.workcheck.domain.assignment.controller;

import com.workcheck.domain.assignment.dto.request.AssignmentRequest;
import com.workcheck.domain.assignment.dto.response.AssignmentResponse;
import com.workcheck.domain.assignment.service.AssignmentService;
import com.workcheck.global.data.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assignment", description = "과제 및 검사 규칙 관리")
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @Operation(summary = "과제와 검사 규칙 등록")
    @PostMapping
    public ResponseEntity<BaseResponse<AssignmentResponse>> createAssignment(
            @AuthenticationPrincipal Long memberId, @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(assignmentService.createAssignment(memberId, request)));
    }

    @Operation(summary = "과제 목록 조회")
    @GetMapping
    public ResponseEntity<BaseResponse<List<AssignmentResponse>>> getAssignments(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(BaseResponse.success(assignmentService.getAssignments(memberId)));
    }

    @Operation(summary = "과제와 검사 규칙 상세 조회")
    @GetMapping("/{assignmentId}")
    public ResponseEntity<BaseResponse<AssignmentResponse>> getAssignment(
            @AuthenticationPrincipal Long memberId, @PathVariable Long assignmentId) {
        return ResponseEntity.ok(BaseResponse.success(assignmentService.getAssignment(memberId, assignmentId)));
    }

    @Operation(summary = "과제와 검사 규칙 전체 수정")
    @PutMapping("/{assignmentId}")
    public ResponseEntity<BaseResponse<AssignmentResponse>> updateAssignment(
            @AuthenticationPrincipal Long memberId, @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(BaseResponse.success(assignmentService.updateAssignment(memberId, assignmentId, request)));
    }

    @Operation(summary = "과제와 검사 규칙 삭제")
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@AuthenticationPrincipal Long memberId, @PathVariable Long assignmentId) {
        assignmentService.deleteAssignment(memberId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
