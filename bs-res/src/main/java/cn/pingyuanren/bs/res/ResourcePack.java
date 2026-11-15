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

/**
 * 运行时资源包：给定逻辑路径（classpath 风格，正斜杠）返回明文字节。
 *
 * <p>P1 阶段是 {@link MemoryResourcePack}（明文内存表，模拟 pak 已解密加载）；
 * P3 阶段换成真正从加密 pak 解密条目的实现。{@link PakFileHandle} 只依赖本接口，
 * 故切换实现无需改动 FileHandle 包装层。</p>
 *
 * @author authorZhao
 * @since 2026-07-17
 */
public interface ResourcePack {

    /** 该逻辑路径是否存在于资源包。 */
    boolean has(String logicalPath);

    /** 读取明文字节；不存在返回 {@code null}。返回的是内部数组，调用方不得修改。 */
    byte[] read(String logicalPath);

    /** 明文长度；不存在返回 0。 */
    long length(String logicalPath);

    /**
     * 是否有任一条目位于目录 {@code dir} 下（pak 无目录条目，只有文件路径，按
     * {@code dir + "/"} 前缀判定）。供 {@link PakFiles} 把 {@code internal("particle")}
     * 这类<b>目录请求</b>也接进 pak——目录句柄只作 {@code child()} 的前缀载体，child
     * 拼出全路径后再查 pak，天然命中（散目录从磁盘删除后目录式访问不落空）。
     *
     * <p>默认 {@code false}；有索引的实现（{@link FileResourcePack} 等）覆写。</p>
     *
     * @since 0.3.7
     */
    default boolean hasDir(String dir) {
        return false;
    }

    /**
     * 目录参数归一：反斜杠→正斜杠、折叠连续斜杠、去首尾斜杠。空串/{@code null} → 空串。
     * {@link #hasDir(String)} 的实现与调用方共用本归一，保证两侧口径一致。
     *
     * @since 0.3.7
     */
    static String normalizeDir(String dir) {
        if (dir == null || dir.isEmpty()) return "";
        String d = dir.replace('\\', '/');
        StringBuilder sb = new StringBuilder(d.length());
        boolean prevSlash = true; // 吃掉前导斜杠
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c == '/') {
                if (prevSlash) continue;
                prevSlash = true;
            } else {
                prevSlash = false;
            }
            sb.append(c);
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/') sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
