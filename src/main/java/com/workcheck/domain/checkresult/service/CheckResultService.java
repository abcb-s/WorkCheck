package com.workcheck.domain.checkresult.service;

import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;
import com.workcheck.domain.checkresult.dto.response.RuleCheckResultResponse;
import com.workcheck.global.data.FileType;
import java.util.List;

public interface CheckResultService {

    CheckResultResponse createCheckResult(Long assignmentId,
                                          String fileName,
                                          FileType fileType,
                                          List<RuleCheckResultResponse> ruleResults);
}
