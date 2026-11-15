/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库
 * Copyright (c) 2026 bs-ui contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package cn.pingyuanren.bs.res;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多分卷合并视图：查找顺序 / hasDir 合并 / 条目统计。
 *
 * @author authorZhao
 * @since 0.3.7
 */
class CompositeResourcePackTest {

    private static byte[] p(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** 用 PakWriter + IdentityCipher 造一个真实 BPK1 分卷（免加密、免 GL）。 */
    private static FileResourcePack volume(String... pathAndSingleContent) {
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
        for (int i = 0; i < pathAndSingleContent.length; i += 2) {
            entries.put(pathAndSingleContent[i], p(pathAndSingleContent[i + 1]));
        }
        byte[] salt = new byte[16];
        return FileResourcePack.open(PakWriter.write(entries, IdentityCipher.INSTANCE, salt), IdentityCipher.INSTANCE);
    }

    @Test
    void lookupFollowsRegistrationOrder() {
        var v1 = volume("a.png", "v1-a", "dup.png", "v1-dup");
        var v2 = volume("b.png", "v2-b", "dup.png", "v2-dup");
        var pack = new CompositeResourcePack(List.of(v1, v2));

        assertTrue(pack.has("a.png"));
        assertTrue(pack.has("b.png"));
        assertTrue(pack.has("dup.png"));
        // 同名条目：先注册的卷赢
        assertEquals("v1-dup", new String(pack.read("dup.png"), StandardCharsets.UTF_8));
        assertEquals(0, pack.length("nope.png"));
        assertNull(pack.read("nope.png"));
        assertFalse(pack.has("nope.png"));
    }

    @Test
    void hasDirMergesAllVolumes() {
        var v1 = volume("particle/lizi_19.png", "x");
        var v2 = volume("ui/button/close.png", "y", "root.png", "z");
        var pack = new CompositeResourcePack(List.of(v1, v2));

        // 目录命中：归一化容忍尾斜杠/反斜杠/重复斜杠
        assertTrue(pack.hasDir("particle"));
        assertTrue(pack.hasDir("particle/"));
        assertTrue(pack.hasDir("particle\\"));
        assertTrue(pack.hasDir("ui/button"));
        assertFalse(pack.hasDir("ui/button/nope"));
        assertFalse(pack.hasDir("nope"));
        // 根下有条目但根目录本身不算目录条目（避免拦截 "" 这类根请求）
        assertFalse(pack.hasDir(""));
    }

    @Test
    void sizeCountsFileVolumesAndVolumeCount() {
        var v1 = volume("a.png", "1", "b.png", "2");
        var v2 = volume("c.png", "3");
        var pack = new CompositeResourcePack(List.of(v1, v2));
        assertEquals(3, pack.size());
        assertEquals(2, pack.volumeCount());
    }
}
