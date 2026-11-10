package com.workcheck.global.util;

import com.workcheck.global.data.FileType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.util.Locale;

/**
 * 확장자와 매직 바이트를 모두 확인해 파일 형식을 판별한다.
 */
public final class FileTypeDetector {

    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};      // PK\3\4
    private static final byte[] ZIP_EMPTY_MAGIC = {0x50, 0x4B, 0x05, 0x06}; // 빈 ZIP
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-

    private FileTypeDetector() {
    }

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
            if (startsWith(content, PDF_MAGIC)) {
                return FileType.PDF;
            }
            throw new CustomException(ErrorCode.INVALID_FILE);
        }
        throw new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    private static boolean startsWith(byte[] content, byte[] magic) {
        if (content.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
