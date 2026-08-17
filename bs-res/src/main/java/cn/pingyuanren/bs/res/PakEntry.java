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
 * BPK1 索引条目（运行时只读）。
 *
 * @author authorZhao
 * @since 2026-07-20
 */
final class PakEntry {

    /** 逻辑路径（classpath 风格，正斜杠）。 */
    final String path;
    /** 条目序号；P3 用于派生 nonce。 */
    final int ordinal;
    /** eflags bit0：条目已压缩。 */
    final boolean compressed;
    /** 相对「blob 区起点」的偏移。 */
    final int blobOff;
    /** cipher 处理（+ 可能压缩）后的字节长度。 */
    final int storedLen;
    /** 明文长度。 */
    final int rawLen;

    PakEntry(String path, int ordinal, boolean compressed, int blobOff, int storedLen, int rawLen) {
        this.path = path;
        this.ordinal = ordinal;
        this.compressed = compressed;
        this.blobOff = blobOff;
        this.storedLen = storedLen;
        this.rawLen = rawLen;
    }
}
