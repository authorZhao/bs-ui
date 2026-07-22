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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

/**
 * 构建期打包器：把多个资源目录打进一个 BPK1 {@code assets.pak}。
 *
 * <p>构建期跑（gradle {@code packResources}），直接读文件系统目录——没有运行时 classpath
 * 目录不可枚举的问题。每个 {@code (dir, prefix)} 对：dir 下（扁平）的每个文件，
 * 逻辑路径 = {@code prefix.isEmpty() ? name : prefix + "/" + name}。</p>
 *
 * <p>用法：{@code PakPacker <outputFile> <dir1> <prefix1> [<dir2> <prefix2> ...]}。
 * 例如把 skin/emoji/icons/demo-i18n 四处资源打成一个 pak。跳过 {@code .ttf}
 *（25MB 字体，烘焙路径不用）。P2 用 {@link IdentityCipher}、不压缩；P3 换 cipher + 开压缩。</p>
 *
 * @author authorZhao
 * @since 2026-07-20
 */
public final class PakPacker {

    public static void main(String[] args) throws IOException {
        if (args.length < 3 || ((args.length - 1) & 1) != 0) {
            System.err.println("用法: PakPacker <outputFile> <dir1> <prefix1> [<dir2> <prefix2> ...]");
            System.exit(2);
        }
        File out = new File(args[0]);
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
        long inBytes = 0;
        for (int i = 1; i < args.length; i += 2) {
            File dir = new File(args[i]);
            String prefix = args[i + 1];
            if (!dir.isDirectory()) {
                System.err.println("[PakPacker] 跳过不存在的目录: " + dir);
                continue;
            }
            File[] files = dir.listFiles(f -> f.isFile() && !f.getName().endsWith(".ttf"));
            if (files == null) continue;
            Arrays.sort(files, Comparator.comparing(File::getName)); // 确定性顺序
            for (File f : files) {
                String path = prefix.isEmpty() ? f.getName() : prefix + "/" + f.getName();
                byte[] data = readAll(f);
                entries.put(path, data);
                inBytes += data.length;
            }
        }

        byte[] salt = new byte[PakFormat.SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] pak = PakWriter.write(entries, IdentityCipher.INSTANCE, salt);

        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(pak);
        }

        System.out.println("PakPacker: 打包 " + entries.size() + " 个资源"
                + "（输入 " + inBytes + " 字节 → pak " + pak.length + " 字节）→ " + out);
    }

    private static byte[] readAll(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            return in.readAllBytes();
        }
    }

    private PakPacker() {}
}
