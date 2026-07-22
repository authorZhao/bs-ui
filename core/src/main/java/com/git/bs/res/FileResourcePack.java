/*
 * bs-ui — Bootstrap 风格的 libGDX Scene2D UI 组件库。
 * Copyright (c) 2026 bs-ui contributors
 *
 * 基于 Apache License 2.0 开源，允许商用、修改和再分发。
 * 使用本库的产品须在“关于”界面标注本项目，详见 LICENSE。
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project home: https://github.com/authorZhao/bs-ui
 */
package com.git.bs.res;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

/**
 * 从 BPK1 字节读取并按条目返回明文。实现 {@link ResourcePack}，运行时替代 P1 的
 * {@link MemoryResourcePack}（P1 是手工塞内存表；本类从真实 pak 文件解析）。
 *
 * <p>整个 pak 一次性读入 {@code byte[]}（~18MB，桌面/web 都可接受），索引在构造期解析完毕，
 * 之后 {@link #read(String)} 做切片 + 解 cipher（+ 可能解压），返回新数组。</p>
 *
 * @author authorZhao
 * @since 2026-07-20
 */
public final class FileResourcePack implements ResourcePack {

    private final byte[] pak;
    private final PakCipher cipher;
    private final byte[] salt;
    private final int blobAreaStart;
    private final Map<String, PakEntry> entries = new LinkedHashMap<>();

    private FileResourcePack(byte[] pak, PakCipher cipher) {
        if (pak.length < PakFormat.HEADER_SIZE) {
            throw new IllegalArgumentException("pak 太小，不是合法 BPK1");
        }
        for (int i = 0; i < 4; i++) {
            if (pak[PakFormat.OFF_MAGIC + i] != PakFormat.MAGIC[i]) {
                throw new IllegalArgumentException("不是 BPK1 pak（magic 不匹配）");
            }
        }
        int version = pak[PakFormat.OFF_VERSION] & 0xff;
        if (version != PakFormat.VERSION) {
            throw new IllegalArgumentException("不支持的 pak 版本: " + version);
        }
        int flags = pak[PakFormat.OFF_FLAGS] & 0xff;
        this.pak = pak;
        this.cipher = cipher;
        this.salt = new byte[PakFormat.SALT_LEN];
        System.arraycopy(pak, PakFormat.OFF_SALT, this.salt, 0, PakFormat.SALT_LEN);

        int indexOff = PakFormat.readU32(pak, PakFormat.OFF_INDEX_OFF);
        int indexLen = PakFormat.readU32(pak, PakFormat.OFF_INDEX_LEN);
        this.blobAreaStart = indexOff + indexLen;

        byte[] indexStored = slice(pak, indexOff, indexLen);
        byte[] indexPlain = cipher.apply(indexStored, this.salt, -1);
        if ((flags & PakFormat.FLAG_INDEX_COMPRESSED) != 0) {
            indexPlain = inflate(indexPlain);
        }

        int p = 0;
        int entryCount = PakFormat.readU32(indexPlain, p);
        p += 4;
        for (int i = 0; i < entryCount; i++) {
            int pathLen = PakFormat.readU16(indexPlain, p);
            p += 2;
            String path = new String(indexPlain, p, pathLen, StandardCharsets.UTF_8);
            p += pathLen;
            int eflags = indexPlain[p++] & 0xff;
            int blobOff = PakFormat.readU32(indexPlain, p);
            p += 4;
            int storedLen = PakFormat.readU32(indexPlain, p);
            p += 4;
            int rawLen = PakFormat.readU32(indexPlain, p);
            p += 4;
            entries.put(path, new PakEntry(path, i,
                    (eflags & PakFormat.EFLAG_COMPRESSED) != 0, blobOff, storedLen, rawLen));
        }
    }

    /** 用默认 cipher（ChaCha20 + PakKeys.KEY）打开。 */
    public static FileResourcePack open(byte[] pak) {
        return open(pak, new ChaCha20Cipher(PakKeys.KEY));
    }

    /** 用指定 cipher 打开（P3 传 ChaCha20）。 */
    public static FileResourcePack open(byte[] pak, PakCipher cipher) {
        return new FileResourcePack(pak, cipher);
    }

    /** 条目数（观测/日志用）。 */
    public int size() {
        return entries.size();
    }

    /** 所有条目路径（按 pak 内顺序，即 ordinal 顺序）。 */
    public java.util.List<String> paths() {
        return new java.util.ArrayList<>(entries.keySet());
    }

    @Override
    public boolean has(String logicalPath) {
        return entries.containsKey(logicalPath);
    }

    @Override
    public long length(String logicalPath) {
        PakEntry e = entries.get(logicalPath);
        return e == null ? 0 : e.rawLen;
    }

    @Override
    public byte[] read(String logicalPath) {
        PakEntry e = entries.get(logicalPath);
        if (e == null) return null;
        byte[] stored = slice(pak, blobAreaStart + e.blobOff, e.storedLen);
        byte[] out = cipher.apply(stored, salt, e.ordinal);
        if (e.compressed) {
            out = inflate(out);
        }
        return out;
    }

    private static byte[] slice(byte[] src, int off, int len) {
        byte[] d = new byte[len];
        System.arraycopy(src, off, d, 0, len);
        return d;
    }

    /** 解 zlib 包装的 DEFLATE（与 PakWriter.deflate 搭配；默认 InflaterInputStream = zlib）。 */
    private static byte[] inflate(byte[] deflated) {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(deflated);
             InflaterInputStream in = new InflaterInputStream(bin)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(deflated.length * 2);
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("pak 条目解压失败", ex);
        }
    }
}
