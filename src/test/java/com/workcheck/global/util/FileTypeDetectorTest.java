package com.workcheck.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workcheck.global.data.FileType;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class FileTypeDetectorTest {

    private static byte[] pdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(text);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] prepend(String prefix, byte[] content) {
        byte[] head = prefix.getBytes(StandardCharsets.US_ASCII);
        byte[] result = new byte[head.length + content.length];
        System.arraycopy(head, 0, result, 0, head.length);
        System.arraycopy(content, 0, result, head.length, content.length);
        return result;
    }

    @Test
    void detect_pdf() throws IOException {
        assertThat(FileTypeDetector.detect("report.pdf", pdf("Hello"))).isEqualTo(FileType.PDF);
    }

    @Test
    void detect_pdfWithBytesBeforeHeader() throws IOException {
        byte[] content = prepend("junk\n", pdf("Hello"));

        assertThat(FileTypeDetector.detect("report.pdf", content)).isEqualTo(FileType.PDF);
        assertThat(PdfTextExtractor.extract(content)).contains("Hello");
    }

    @Test
    void detect_pdfHeaderAfterFirst1024Bytes_throwsInvalidFile() throws IOException {
        byte[] content = prepend("x".repeat(1024), pdf("Hello"));

        assertThatThrownBy(() -> FileTypeDetector.detect("report.pdf", content))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }
}
