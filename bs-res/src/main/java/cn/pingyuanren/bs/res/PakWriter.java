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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把 {@code path -> 明文字节}（按插入顺序）构建成 BPK1 字节流。
 *
 * <p>P2：{@link IdentityCipher}、条目不压缩（eflags=0）。格式本身支持压缩/加密，
 * P3 换 cipher + 打开压缩即可，本类无需改动。</p>
 *
 * @author authorZhao
 * @since 2026-07-20
 */
public final class PakWriter {

    /**
     * @param entries 按插入顺序的 path -> 明文字节（调用方不再持有/改写这些数组）
     * @param cipher  对索引和条目做对称处理（P2 用 {@link IdentityCipher#INSTANCE}）
     * @param salt    16 字节盐（写入 header；P3 参与派生 nonce），长度不足 16 补 0、超出截断
     * @return 完整的 BPK1 字节流
     */
    public static byte[] write(LinkedHashMap<String, byte[]> entries, PakCipher cipher, byte[] salt) {
        int n = entries.size();
        byte[][] pathBytes = new byte[n][];
        byte[][] stored = new byte[n][];
        int[] rawLens = new int[n];

        boolean[] compressed = new boolean[n];
        int ordinal = 0;
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String path = e.getKey();
            pathBytes[ordinal] = path.getBytes(StandardCharsets.UTF_8);
            byte[] raw = e.getValue();
            rawLens[ordinal] = raw.length;
            // 文本类先 DEFLATE 再加密（压缩必须在加密前——密文不可压）；PNG/wasm 已压缩不压。
            boolean c = compressible(path);
            compressed[ordinal] = c;
            byte[] pre = c ? deflate(raw) : raw;
            stored[ordinal] = cipher.apply(pre, salt, ordinal);
            ordinal++;
        }

        // 索引明文：blobOff 用「相对 blob 区起点」的偏移，与索引大小解耦
        int indexPlainLen = 4; // entryCount
        int[] blobRelOff = new int[n];
        int cum = 0;
        for (int i = 0; i < n; i++) {
            blobRelOff[i] = cum;
            cum += stored[i].length;
            indexPlainLen += 2 + pathBytes[i].length + 1 + 4 + 4 + 4;
        }
        byte[] indexPlain = new byte[indexPlainLen];
        int p = 0;
        PakFormat.writeU32(indexPlain, p, n);
        p += 4;
        for (int i = 0; i < n; i++) {
            PakFormat.writeU16(indexPlain, p, pathBytes[i].length);
            p += 2;
            System.arraycopy(pathBytes[i], 0, indexPlain, p, pathBytes[i].length);
            p += pathBytes[i].length;
            indexPlain[p++] = (byte) (compressed[i] ? PakFormat.EFLAG_COMPRESSED : 0); // eflags bit0：是否 DEFLATE
            PakFormat.writeU32(indexPlain, p, blobRelOff[i]);
            p += 4;
            PakFormat.writeU32(indexPlain, p, stored[i].length);
            p += 4;
            PakFormat.writeU32(indexPlain, p, rawLens[i]);
            p += 4;
        }

        // cipher 处理索引（ordinal=-1）；长度不变（流密码契约），故 indexLen == indexPlainLen
        byte[] indexStored = cipher.apply(indexPlain, salt, -1);
        int indexLen = indexStored.length;
        int flags = 0; // 索引不压缩（P3 可打开）

        // 拼装：header + indexStored + blobs
        byte[] header = new byte[PakFormat.HEADER_SIZE];
        System.arraycopy(PakFormat.MAGIC, 0, header, PakFormat.OFF_MAGIC, 4);
        header[PakFormat.OFF_VERSION] = (byte) PakFormat.VERSION;
        header[PakFormat.OFF_FLAGS] = (byte) flags;
        int saltCopy = Math.min(salt.length, PakFormat.SALT_LEN);
        System.arraycopy(salt, 0, header, PakFormat.OFF_SALT, saltCopy);
        PakFormat.writeU32(header, PakFormat.OFF_INDEX_OFF, PakFormat.HEADER_SIZE);
        PakFormat.writeU32(header, PakFormat.OFF_INDEX_LEN, indexLen);

        ByteArrayOutputStream out = new ByteArrayOutputStream(PakFormat.HEADER_SIZE + indexLen + cum);
        out.write(header, 0, header.length);
        out.write(indexStored, 0, indexStored.length);
        for (int i = 0; i < n; i++) {
            out.write(stored[i], 0, stored[i].length);
        }
        return out.toByteArray();
    }

    /** 文本类压缩收益大；PNG/wasm/ttf 等已压缩、再压几乎无收益甚至变大，跳过。 */
    private static boolean compressible(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = path.substring(dot).toLowerCase(java.util.Locale.ROOT);
        return ext.equals(".json") || ext.equals(".atlas") || ext.equals(".fnt")
                || ext.equals(".properties") || ext.equals(".txt");
    }

    /** DEFLATE 压缩（zlib 包装，与 FileResourcePack 的 InflaterInputStream 默认搭配）。 */
    private static byte[] deflate(byte[] data) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.DeflaterOutputStream dos = new java.util.zip.DeflaterOutputStream(
                out, new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION))) {
            dos.write(data);
        } catch (java.io.IOException ex) {
            throw new RuntimeException("DEFLATE 压缩失败", ex);
        }
        return out.toByteArray();
    }

    private PakWriter() {}
}
