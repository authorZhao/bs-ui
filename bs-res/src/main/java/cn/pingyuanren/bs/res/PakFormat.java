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

import java.nio.charset.StandardCharsets;

/**
 * BPK1 资源包二进制格式常量 + 小端读写工具。
 *
 * <p>布局（小端序），详见 docs/resource-encryption-design.md 第 5 节：</p>
 * <pre>
 * Header(32):
 *   0  4  magic "BPK1"
 *   4  1  version(=1)
 *   5  1  flags   bit0: 索引已压缩
 *   8  16 salt
 *  24  4  indexOff
 *  28  4  indexLen  (cipher 处理后的索引字节长度)
 * Index(在 indexOff)：
 *   u32 entryCount; 每条: u16 pathLen + path + u8 eflags + u32 blobOff + u32 storedLen + u32 rawLen
 * Blobs：每条 storedLen 字节（cipher 处理后，可能压缩）
 *
 * blobOff 是相对「blob 区起点」(= indexOff + indexLen) 的偏移，与索引大小解耦，
 * 这样 P3 给索引加压缩时不会有循环依赖。
 * </pre>
 *
 * @author authorZhao
 * @since 2026-07-20
 */
final class PakFormat {

    static final byte[] MAGIC = "BPK1".getBytes(StandardCharsets.US_ASCII);
    static final int VERSION = 1;
    static final int HEADER_SIZE = 32;
    static final int SALT_LEN = 16;

    static final int OFF_MAGIC = 0;
    static final int OFF_VERSION = 4;
    static final int OFF_FLAGS = 5;
    static final int OFF_SALT = 8;
    static final int OFF_INDEX_OFF = 24;
    static final int OFF_INDEX_LEN = 28;

    /** header flags bit0：索引已压缩。 */
    static final int FLAG_INDEX_COMPRESSED = 1;
    /** 条目 eflags bit0：该条目已压缩。 */
    static final int EFLAG_COMPRESSED = 1;

    private PakFormat() {}

    static int readU32(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8)
                | ((b[off + 2] & 0xff) << 16) | ((b[off + 3] & 0xff) << 24);
    }

    static void writeU32(byte[] b, int off, int v) {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >>> 8);
        b[off + 2] = (byte) (v >>> 16);
        b[off + 3] = (byte) (v >>> 24);
    }

    static int readU16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    static void writeU16(byte[] b, int off, int v) {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >>> 8);
    }
}
