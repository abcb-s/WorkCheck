package com.workcheck.domain.member.dto.response;

public record SigninResponse(
        String accessToken
) {

    public static SigninResponse from(String accessToken) {
        return new SigninResponse(accessToken);
    }
}
