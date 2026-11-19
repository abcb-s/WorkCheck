package com.workcheck.domain.check.service;

import com.workcheck.domain.check.dto.response.CheckResponse;
import com.workcheck.domain.check.dto.response.CheckSummaryResponse;
import com.workcheck.global.data.CheckStatus;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CheckService {

    CheckResponse check(Long memberId, Long assignmentId, MultipartFile file);

    List<CheckSummaryResponse> getMyChecks(Long memberId, CheckStatus status);

    List<CheckSummaryResponse> getChecks(Long memberId, Long assignmentId, CheckStatus status);

    CheckResponse getCheck(Long memberId, Long assignmentId, Long checkId);
}
