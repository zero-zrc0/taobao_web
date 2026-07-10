package org.taobao.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class OssUrlUtils {

    private OssUrlUtils() {
    }

    public static String extractObjectKey(String value) {
        if (value == null || value.isBlank() || (!value.startsWith("http://") && !value.startsWith("https://"))) {
            return value;
        }

        try {
            String path = URI.create(value).getPath();
            return path == null || path.isBlank() ? value : path.replaceFirst("^/+", "");
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    public static String extractObjectKeys(String values) {
        if (values == null || values.isBlank()) {
            return values;
        }

        return Arrays.stream(values.split(","))
                .map(String::trim)
                .map(OssUrlUtils::extractObjectKey)
                .collect(Collectors.joining(","));
    }
}
