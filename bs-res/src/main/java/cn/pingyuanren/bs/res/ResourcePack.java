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
}
