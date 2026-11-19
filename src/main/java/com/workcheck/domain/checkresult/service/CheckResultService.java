package com.workcheck.domain.checkresult.service;

import com.workcheck.domain.checkresult.dto.request.CheckResultRequest;
import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;

public interface CheckResultService {

    CheckResultResponse createCheckResult(CheckResultRequest request);
}
