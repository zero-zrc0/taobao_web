package org.taobao.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OssUrlUtilsTest {

    @Test
    void extractsObjectKeyFromFullUrl() {
        assertEquals("2026/07/image.png", OssUrlUtils.extractObjectKey(
                "https://taobao-web.oss-cn-beijing.aliyuncs.com/2026/07/image.png"));
    }

    @Test
    void extractsEveryObjectKeyFromUrlList() {
        assertEquals("2026/07/one.png,2026/07/two.png", OssUrlUtils.extractObjectKeys(
                "https://taobao-web.oss-cn-beijing.aliyuncs.com/2026/07/one.png,"
                        + "https://taobao-web.oss-cn-beijing.aliyuncs.com/2026/07/two.png"));
    }

    @Test
    void preservesRelativeObjectKey() {
        assertEquals("2026/07/image.png", OssUrlUtils.extractObjectKey("2026/07/image.png"));
    }

    @Test
    void extractsFirstObjectKeyFromJsonImageList() {
        assertEquals("2026/07/one.png", OssUrlUtils.extractFirstObjectKey(
                "[\"https://taobao-web.oss-cn-beijing.aliyuncs.com/2026/07/one.png\","
                        + "\"https://taobao-web.oss-cn-beijing.aliyuncs.com/2026/07/two.png\"]"));
    }
}
