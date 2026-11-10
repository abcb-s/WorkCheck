package com.workcheck.global.util;

import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * PDF 텍스트를 추출한다. 추출 가능한 텍스트가 없으면(스캔 PDF) 빈 문자열을 반환한다.
 */
public final class PdfTextExtractor {

    private PdfTextExtractor() {
    }

    public static String extract(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            String text = new PDFTextStripper().getText(document);
            return text == null ? "" : text.strip();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_FILE, e);
        }
    }

    public static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
