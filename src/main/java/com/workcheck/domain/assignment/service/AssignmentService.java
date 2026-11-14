package com.workcheck.domain.assignment.service;

import com.workcheck.domain.assignment.dto.request.AssignmentRequest;
import com.workcheck.domain.assignment.dto.response.AssignmentResponse;
import java.util.List;

public interface AssignmentService {

    AssignmentResponse createAssignment(Long memberId, AssignmentRequest request);

    List<AssignmentResponse> getAssignments(Long memberId);

    AssignmentResponse getAssignment(Long memberId, Long assignmentId);

    AssignmentResponse updateAssignment(Long memberId, Long assignmentId, AssignmentRequest request);

    void deleteAssignment(Long memberId, Long assignmentId);
}
