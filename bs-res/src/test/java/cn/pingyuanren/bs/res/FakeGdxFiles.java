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

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试用内存 Files：path → bytes 表驱动，免 GDX 后端（无 GL/无磁盘）。
 * 仅供 pak 链路单测（{@code VolumePakBootstrap}/{@code PakFiles} 只依赖
 * {@code internal/getFileHandle/classpath} 的 {@code exists/readBytes/readString}）。
 *
 * @author authorZhao
 * @since 0.3.7
 */
final class FakeGdxFiles implements Files {

    private final Map<String, byte[]> files = new HashMap<>();

    void put(String path, byte[] data) {
        files.put(path.replace('\\', '/'), data);
    }

    void putText(String path, String text) {
        put(path, text.getBytes(StandardCharsets.UTF_8));
    }

    boolean has(String path) {
        return files.containsKey(path.replace('\\', '/'));
    }

    private FileHandle handle(String path) {
        return new FakeHandle(path, files.get(path.replace('\\', '/')));
    }

    @Override
    public FileHandle internal(String path) {
        return handle(path);
    }

    @Override
    public FileHandle getFileHandle(String path, FileType type) {
        return handle(path);
    }

    @Override
    public FileHandle classpath(String path) {
        return handle(path);
    }

    @Override
    public FileHandle external(String path) {
        throw new UnsupportedOperationException("测试不覆盖 external");
    }

    @Override
    public FileHandle absolute(String path) {
        throw new UnsupportedOperationException("测试不覆盖 absolute");
    }

    @Override
    public FileHandle local(String path) {
        throw new UnsupportedOperationException("测试不覆盖 local");
    }

    @Override
    public String getExternalStoragePath() {
        return null;
    }

    @Override
    public boolean isExternalStorageAvailable() {
        return false;
    }

    @Override
    public String getLocalStoragePath() {
        return null;
    }

    @Override
    public boolean isLocalStorageAvailable() {
        return false;
    }

    /** 内存字节句柄：exists/readBytes/readString/read 覆盖，其余走 FileHandle 默认。 */
    static final class FakeHandle extends FileHandle {

        private final byte[] data;

        FakeHandle(String path, byte[] data) {
            super(path, FileType.Internal);
            this.data = data;
        }

        @Override
        public boolean exists() {
            return data != null;
        }

        @Override
        public byte[] readBytes() {
            return data;
        }

        @Override
        public InputStream read() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public String readString(String encoding) {
            return new String(data, StandardCharsets.UTF_8);
        }
    }
}
