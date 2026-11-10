package com.workcheck.global.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.workcheck.global.exception.status_code.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BaseResponse<T>(T data, ErrorBody error) {

    public record ErrorBody(String code, String message) {
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(data, null);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(null, new ErrorBody(errorCode.getCode(), message));
    }
}
