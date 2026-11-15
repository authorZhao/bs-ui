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

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PakFiles 目录感知回归：{@code internal("particle")} 这类目录请求接进 pak，
 * child 拼全路径后命中（对应 cwwg9 2026-11-03 修的「散目录删除后目录式访问落空」bug）。
 *
 * @author authorZhao
 * @since 0.3.7
 */
class PakFilesDirTest {

    private static FileResourcePack packWithParticle() {
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("particle/lizi_19.png", "lizi".getBytes(StandardCharsets.UTF_8));
        entries.put("particle/lizi_20.png", "lizi20".getBytes(StandardCharsets.UTF_8));
        entries.put("skin/bs.json", "{}".getBytes(StandardCharsets.UTF_8));
        byte[] salt = new byte[16];
        return FileResourcePack.open(PakWriter.write(entries, IdentityCipher.INSTANCE, salt),
                IdentityCipher.INSTANCE);
    }

    @Test
    void directoryRequestRoutesIntoPakAndChildResolves() {
        var delegate = new FakeGdxFiles();
        var files = new PakFiles(delegate, packWithParticle());

        // 目录请求：hasDir 命中 → PakFileHandle（目录句柄只作 child 前缀载体）
        FileHandle dir = files.internal("particle");
        assertInstanceOf(PakFileHandle.class, dir, "目录请求应接进 pak");
        // child 拼全路径后再查 pak，天然命中（loadEmitterImages 的标准链路）
        FileHandle child = dir.child("lizi_19.png");
        assertInstanceOf(PakFileHandle.class, child);
        assertTrue(child.exists());
        assertEquals("lizi", new String(child.readBytes(), StandardCharsets.UTF_8));

        // 子目录多级
        FileHandle nested = files.internal("particle").child("sub");
        assertFalse(nested.exists(), "pak 内不存在的子路径 exists=false");
    }

    @Test
    void threeLookupMethodsAreDirAware() {
        var delegate = new FakeGdxFiles();
        var files = new PakFiles(delegate, packWithParticle());

        assertInstanceOf(PakFileHandle.class, files.internal("particle"));
        assertInstanceOf(PakFileHandle.class, files.getFileHandle("particle", FileType.Internal));
        assertInstanceOf(PakFileHandle.class, files.classpath("particle"));
        // 非 Internal 类型不拦
        assertFalse(files.getFileHandle("particle", FileType.Absolute) instanceof PakFileHandle);
    }

    @Test
    void nonPakPathsFallThroughToDelegate() {
        var delegate = new FakeGdxFiles();
        delegate.putText("saves/slot1.json", "{}");
        var files = new PakFiles(delegate, packWithParticle());

        // pak 外文件：透传 delegate（仍是普通句柄）
        FileHandle save = files.internal("saves/slot1.json");
        assertFalse(save instanceof PakFileHandle);
        assertTrue(save.exists());
        // pak 外目录：透传（不产生伪 PakFileHandle）
        assertFalse(files.internal("saves") instanceof PakFileHandle);
    }
}
