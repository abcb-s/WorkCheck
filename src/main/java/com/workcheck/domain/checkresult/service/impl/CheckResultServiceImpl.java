package com.workcheck.domain.checkresult.service.impl;

import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;
import com.workcheck.domain.checkresult.dto.response.RuleCheckResultResponse;
import com.workcheck.domain.checkresult.service.CheckResultService;
import com.workcheck.global.data.FileType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckResultServiceImpl implements CheckResultService {

    @Override
    public CheckResultResponse createCheckResult(Long assignmentId,
                                                 String fileName,
                                                 FileType fileType,
                                                 List<RuleCheckResultResponse> ruleResults) {
        if (ruleResults.isEmpty()) {
            throw new CustomException(ErrorCode.NO_APPLICABLE_RULE);
        }

        boolean allPassed = true;
        for (RuleCheckResultResponse ruleResult : ruleResults) {
            if (!ruleResult.passed()) {
                allPassed = false;
                break;
            }
        }

        return new CheckResultResponse(assignmentId, fileName, fileType, allPassed, ruleResults);
    }
}
