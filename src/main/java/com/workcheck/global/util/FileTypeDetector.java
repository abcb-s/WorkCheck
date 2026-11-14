package com.workcheck.global.util;

import com.workcheck.global.data.FileType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileTypeDetector {

    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] ZIP_EMPTY_MAGIC = {0x50, 0x4B, 0x05, 0x06};
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D};
    private static final int PDF_HEADER_SEARCH_LIMIT = 1024;

    /** 확장자와 매직 바이트로 파일 형식을 판별한다. */
    public static FileType detect(String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".zip")) {
            if (startsWith(content, ZIP_MAGIC) || startsWith(content, ZIP_EMPTY_MAGIC)) {
                return FileType.ZIP;
            }
            throw new CustomException(ErrorCode.INVALID_FILE);
        }
        if (lower.endsWith(".pdf")) {
            if (containsWithin(content, PDF_MAGIC, PDF_HEADER_SEARCH_LIMIT)) {
                return FileType.PDF;
            }
            throw new CustomException(ErrorCode.INVALID_FILE);
        }
        throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    private static boolean startsWith(byte[] content, byte[] magic) {
        return matchesAt(content, magic, 0);
    }

    private static boolean containsWithin(byte[] content, byte[] magic, int limit) {
        for (int offset = 0; offset < limit; offset++) {
            if (matchesAt(content, magic, offset)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAt(byte[] content, byte[] magic, int offset) {
        if (content.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
