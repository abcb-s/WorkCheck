package com.workcheck.global.exception.status_code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않아요."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "업로드한 파일이 비어 있어요."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "INVALID_FILE", "파일이 손상되었거나 읽을 수 없어요."),
    NO_APPLICABLE_RULE(HttpStatus.BAD_REQUEST, "NO_APPLICABLE_RULE", "이 파일 형식에 적용할 검사 규칙이 없어요."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요해요."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않아요."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없어요."),
    INVALID_CSRF(HttpStatus.FORBIDDEN, "INVALID_CSRF", "보안 토큰이 없거나 만료됐어요. 새로고침 후 다시 시도해 주세요."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없어요."),
    ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ASSIGNMENT_NOT_FOUND", "과제를 찾을 수 없어요."),
    CHECK_NOT_FOUND(HttpStatus.NOT_FOUND, "CHECK_NOT_FOUND", "검사 기록을 찾을 수 없어요."),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일이에요."),

    // 413
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "파일 크기가 너무 커요."),

    // 415
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE", "ZIP 또는 PDF 파일만 업로드할 수 있어요."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
