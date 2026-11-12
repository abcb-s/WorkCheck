package com.workcheck.global.util;

import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

public final class ZipInspector {

    private static final String MACOS_METADATA_DIR = "__MACOSX/";
    private static final String DS_STORE = ".DS_Store";

    private ZipInspector() {
    }

    /** ZIP 내부 엔트리 경로 목록을 읽는다. */
    public static List<String> listEntries(byte[] content) {
        try (ZipFile zipFile = ZipFile.builder()
                .setSeekableByteChannel(new SeekableInMemoryByteChannel(content))
                .get()) {
            List<String> names = new ArrayList<>();
            for (ZipArchiveEntry entry : Collections.list(zipFile.getEntries())) {
                names.add(normalize(entry.getName()));
            }
            return names;
        } catch (IOException | RuntimeException e) {
            throw new CustomException(ErrorCode.INVALID_FILE, e);
        }
    }

    /** 필수 경로가 ZIP에 있는지 확인하고, 공통 최상위 폴더를 벗긴 경로로도 확인한다. */
    public static boolean containsPath(List<String> entries, String requiredPath) {
        String target = trimSlashes(normalize(requiredPath));
        if (target.isEmpty()) {
            return false;
        }
        if (matches(entries, target)) {
            return true;
        }
        String root = commonTopLevelFolder(entries);
        if (root == null) {
            return false;
        }
        List<String> stripped = entries.stream()
                .filter(name -> name.startsWith(root) && name.length() > root.length())
                .map(name -> name.substring(root.length()))
                .toList();
        return matches(stripped, target);
    }

    private static boolean matches(List<String> entries, String target) {
        for (String entry : entries) {
            String name = trimSlashes(entry);
            if (name.equals(target) || name.startsWith(target + "/")) {
                return true;
            }
        }
        return false;
    }

    /** 공통 최상위 폴더("folder/")를 반환하고, 없으면 null. */
    private static String commonTopLevelFolder(List<String> entries) {
        String root = null;
        for (String entry : entries) {
            if (isOsMetadata(entry)) {
                continue;
            }
            int slash = entry.indexOf('/');
            if (slash < 0) {
                return null;
            }
            String top = entry.substring(0, slash + 1);
            if (root == null) {
                root = top;
            } else if (!root.equals(top)) {
                return null;
            }
        }
        return root;
    }

    /** macOS Finder가 압축 시 끼워 넣는 메타데이터(__MACOSX/, .DS_Store)인지 확인한다. */
    private static boolean isOsMetadata(String entry) {
        return entry.startsWith(MACOS_METADATA_DIR)
                || entry.equals(DS_STORE)
                || entry.endsWith("/" + DS_STORE);
    }

    private static String normalize(String path) {
        String p = path.replace('\\', '/');
        while (p.startsWith("./")) {
            p = p.substring(2);
        }
        return p;
    }

    private static String trimSlashes(String path) {
        int start = 0;
        int end = path.length();
        while (start < end && path.charAt(start) == '/') start++;
        while (end > start && path.charAt(end - 1) == '/') end--;
        return path.substring(start, end);
    }
}
