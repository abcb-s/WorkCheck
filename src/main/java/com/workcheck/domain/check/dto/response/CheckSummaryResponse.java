package com.workcheck.domain.check.dto.response;

import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.global.data.CheckStatus;
import java.time.LocalDateTime;

public record CheckSummaryResponse(
        Long id,
        String fileName,
        CheckStatus status,
        LocalDateTime createdAt
) {

    public static CheckSummaryResponse from(CheckHistory history) {
        return new CheckSummaryResponse(history.getId(), history.getFileName(), history.getStatus(),
                history.getCreatedAt());
    }
}
