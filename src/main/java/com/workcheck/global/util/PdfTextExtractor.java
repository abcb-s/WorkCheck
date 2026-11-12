package com.workcheck.global.util;

import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PdfTextExtractor {

    /** PDF 텍스트를 추출하고, 텍스트가 없으면 빈 문자열을 반환한다. */
    public static String extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return new PDFTextStripper().getText(document).strip();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_FILE, e);
        }
    }
}
