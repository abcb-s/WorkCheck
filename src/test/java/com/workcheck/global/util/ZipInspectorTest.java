package com.workcheck.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class ZipInspectorTest {

    private static byte[] zip(String... names) throws IOException {
        return zip(StandardCharsets.UTF_8, names);
    }

    private static byte[] zip(Charset charset, String... names) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out, charset)) {
            for (String name : names) {
                zos.putNextEntry(new ZipEntry(name));
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    void containsPath_directMatch() throws IOException {
        List<String> entries = ZipInspector.listEntries(zip("src/Main.java", "data.sql"));

        assertThat(ZipInspector.containsPath(entries, "data.sql")).isTrue();
        assertThat(ZipInspector.containsPath(entries, "src/")).isTrue();
        assertThat(ZipInspector.containsPath(entries, "README.md")).isFalse();
    }

    @Test
    void containsPath_githubZip_stripsTopLevelFolder() throws IOException {
        List<String> entries = ZipInspector.listEntries(zip("repo-main/", "repo-main/src/data.sql"));

        assertThat(ZipInspector.containsPath(entries, "src/data.sql")).isTrue();
    }

    @Test
    void containsPath_macOsZip_ignoresMetadataWhenStripping() throws IOException {
        List<String> entries = ZipInspector.listEntries(zip(
                "proj/", "proj/data.sql", "proj/.DS_Store",
                "__MACOSX/", "__MACOSX/proj/._data.sql", ".DS_Store"));

        assertThat(ZipInspector.containsPath(entries, "data.sql")).isTrue();
        assertThat(ZipInspector.containsPath(entries, "missing.sql")).isFalse();
    }

    @Test
    void containsPath_multipleTopLevelFolders_doesNotStrip() throws IOException {
        List<String> entries = ZipInspector.listEntries(zip("a/data.sql", "b/other.sql"));

        assertThat(ZipInspector.containsPath(entries, "data.sql")).isFalse();
    }

    @Test
    void listEntries_cp949Names_decodesKorean() throws IOException {
        List<String> entries = ZipInspector.listEntries(zip(Charset.forName("MS949"), "과제/보고서.pdf"));

        assertThat(entries).containsExactly("과제/보고서.pdf");
        assertThat(ZipInspector.containsPath(entries, "보고서.pdf")).isTrue();
    }

    @Test
    void listEntries_utf8Names_keepsKorean() throws IOException {
        List<String> entries = ZipInspector.listEntries(zip("과제/보고서.pdf"));

        assertThat(entries).containsExactly("과제/보고서.pdf");
    }

    @Test
    void listEntries_corruptedZip_throwsInvalidFile() {
        assertThatThrownBy(() -> ZipInspector.listEntries(new byte[]{0x50, 0x4B, 0x03, 0x04, 1, 2, 3}))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }
}
