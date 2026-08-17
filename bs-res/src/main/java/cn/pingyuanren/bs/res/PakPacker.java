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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

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
        int dupCount = 0;
        long dupBytes = 0;
        for (int i = 1; i < args.length; i += 2) {
            File dir = new File(args[i]);
            String prefix = args[i + 1];
            if (!dir.isDirectory()) {
                System.err.println("[PakPacker] ⚠ 跳过不存在的目录: " + dir);
                continue;
            }
            File[] files = dir.listFiles(f -> f.isFile() && !f.getName().endsWith(".ttf"));
            if (files == null) continue;
            Arrays.sort(files, Comparator.comparing(File::getName)); // 确定性顺序
            // 检测子目录：PakPacker 是扁平扫描（不递归），子目录里的文件会被忽略
            File[] subdirs = dir.listFiles(File::isDirectory);
            if (subdirs != null && subdirs.length > 0) {
                System.out.println("[PakPacker] ⚠ " + dir + " 含 " + subdirs.length
                        + " 个子目录将被忽略（扁平扫描不递归）: "
                        + Arrays.stream(subdirs).map(File::getName)
                                .sorted().collect(java.util.stream.Collectors.joining(", ")));
            }
            System.out.println("[PakPacker] 扫描目录: " + dir + " (prefix=" + prefix + ", " + files.length + " 文件)");
            for (File f : files) {
                String path = prefix.isEmpty() ? f.getName() : prefix + "/" + f.getName();
                byte[] data = readAll(f);
                // 重复检测：同逻辑路径被前面的目录打过 → 覆盖（后者赢），输出 warn
                if (entries.containsKey(path)) {
                    dupCount++;
                    dupBytes += entries.get(path).length;   // 被覆盖的旧大小
                    System.out.println("[PakPacker]   ⚠ 覆盖重复: " + path
                            + " (旧 " + entries.get(path).length + "B ← 新 " + data.length + "B)");
                }
                entries.put(path, data);
                inBytes += data.length;
                System.out.println("  " + path + "  (" + data.length + " B)");
            }
        }

        byte[] salt = new byte[PakFormat.SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] pak = PakWriter.write(entries, new ChaCha20Cipher(PakKeys.KEY), salt);

        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(pak);
        }

        // 汇总
        System.out.println("[PakPacker] ===== 打包汇总 =====");
        System.out.println("[PakPacker] 条目数: " + entries.size()
                + (dupCount > 0 ? " (其中 " + dupCount + " 个重复覆盖，旧数据 " + dupBytes + " B 被丢弃)" : ""));
        System.out.println("[PakPacker] 输入: " + inBytes + " 字节 → pak: " + pak.length + " 字节"
                + " (加密膨胀约 " + (pak.length - inBytes) + " 字节)");
        System.out.println("[PakPacker] 输出: " + out);
    }

    private static byte[] readAll(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            return in.readAllBytes();
        }
    }

    private PakPacker() {}
}
