package com.workcheck.domain.assignment.service;

import com.workcheck.domain.assignment.dto.request.AssignmentRequest;
import com.workcheck.domain.assignment.dto.response.AssignmentResponse;
import java.util.List;

public interface AssignmentService {

    AssignmentResponse createAssignment(AssignmentRequest request);

    List<AssignmentResponse> getAssignments();

    AssignmentResponse getAssignment(Long assignmentId);

}
