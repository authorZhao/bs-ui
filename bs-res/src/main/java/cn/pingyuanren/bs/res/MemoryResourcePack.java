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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 明文内存资源包（P1 spike 用）。
 *
 * <p>把若干 {@code 逻辑路径 → 明文字节} 装进内存表，{@link #read(String)} 直接返回。
 * 模拟“加密 pak 已在启动时读入并解密”的状态——P3 会用真正的解密实现替换它。</p>
 *
 * @author authorZhao
 * @since 2026-07-17
 */
public final class MemoryResourcePack implements ResourcePack {

    private final Map<String, byte[]> entries = new LinkedHashMap<>();

    /** 装入一条资源（路径按正斜杠归一）。 */
    public void put(String logicalPath, byte[] data) {
        entries.put(normalize(logicalPath), data);
    }

    @Override
    public boolean has(String logicalPath) {
        return entries.containsKey(normalize(logicalPath));
    }

    @Override
    public byte[] read(String logicalPath) {
        return entries.get(normalize(logicalPath));
    }

    @Override
    public long length(String logicalPath) {
        byte[] d = entries.get(normalize(logicalPath));
        return d == null ? 0 : d.length;
    }

    /** 条目数（日志/观测用）。 */
    public int size() {
        return entries.size();
    }

    /** 路径归一：反斜杠 → 正斜杠。与 {@link PakFileHandle} 的路径算术保持一致。 */
    static String normalize(String p) {
        return p == null ? "" : p.replace('\\', '/');
    }
}
