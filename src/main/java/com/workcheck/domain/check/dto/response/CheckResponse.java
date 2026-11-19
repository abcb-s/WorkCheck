package com.workcheck.domain.check.dto.response;

import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.global.data.CheckStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CheckResponse(
        Long id,
        String fileName,
        CheckStatus status,
        List<CheckResultResponse> results,
        LocalDateTime createdAt
) {

    public static CheckResponse from(CheckHistory history) {
        List<CheckResultResponse> results = history.getResults().stream()
                .map(CheckResultResponse::from)
                .toList();
        return new CheckResponse(history.getId(), history.getFileName(), history.getStatus(), results,
                history.getCreatedAt());
    }
}
