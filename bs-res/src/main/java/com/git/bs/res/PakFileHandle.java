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

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 由 {@link ResourcePack} 支撑的 {@link FileHandle}：read 系列方法返回包内明文字节，
 * child/sibling/parent 仍返回 {@link PakFileHandle}（保证 skin→atlas→png→fnt 这类
 * sibling 链不断裂、全部走包内字节）。
 *
 * <p><b>覆盖面</b>（libGDX 的 Skin / TextureAtlas / BitmapFont / Texture / Pixmap 实际用到的）：
 * read / readBytes / exists / length / isDirectory / list / child / sibling / parent。
 * 其余（file / lastModified）回退到构造时传入的 {@code delegate} 真实句柄。</p>
 *
 * <p><b>注意</b>：read 返回的是包内数组本体（多读共享、各 stream 自带游标，安全）；
 * readBytes 同样返回本体，调用方约定不修改返回数组。</p>
 *
 * @author authorZhao
 * @since 2026-07-17
 */
public final class PakFileHandle extends FileHandle {

    private final ResourcePack pack;
    private final String logicalPath;
    /** 真实平台句柄，仅用于 file()/lastModified() 等未覆盖方法的回退。 */
    private final FileHandle delegate;

    public PakFileHandle(String path, ResourcePack pack, FileHandle delegate) {
        super(path, FileType.Internal);
        this.logicalPath = normalize(path);
        this.pack = pack;
        this.delegate = delegate;
    }

    // =================== 读 ===================

    @Override
    public InputStream read() {
        byte[] data = pack.read(logicalPath);
        if (data == null) throw new GdxRuntimeException("pak missing: " + logicalPath);
        served.add(logicalPath);
        return new ByteArrayInputStream(data);
    }

    @Override
    public byte[] readBytes() {
        byte[] data = pack.read(logicalPath);
        if (data == null) throw new GdxRuntimeException("pak missing: " + logicalPath);
        served.add(logicalPath);
        return data;
    }

    @Override
    public boolean exists() {
        return pack.has(logicalPath);
    }

    @Override
    public long length() {
        return pack.length(logicalPath);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public FileHandle[] list() {
        return new FileHandle[0];
    }

    // =================== 导航：必须保持 PakFileHandle 类型，否则 sibling 链断 ===================

    @Override
    public FileHandle child(String name) {
        return new PakFileHandle(childPath(logicalPath, name), pack, delegate.child(name));
    }

    @Override
    public FileHandle sibling(String name) {
        return new PakFileHandle(siblingPath(logicalPath, name), pack, delegate.sibling(name));
    }

    @Override
    public FileHandle parent() {
        return new PakFileHandle(parentPath(logicalPath), pack, delegate.parent());
    }

    // =================== 回退到真实句柄 ===================

    @Override
    public java.io.File file() {
        return delegate.file();
    }

    @Override
    public long lastModified() {
        return delegate.lastModified();
    }

    // =================== 路径算术（统一正斜杠，和包内 lookup 一致）===================

    static String normalize(String p) {
        return p == null ? "" : p.replace('\\', '/');
    }

    static String childPath(String parent, String name) {
        name = normalize(name);
        if (parent.isEmpty()) return name;
        return parent.endsWith("/") ? parent + name : parent + "/" + name;
    }

    static String parentPath(String p) {
        p = normalize(p);
        int idx = p.lastIndexOf('/');
        return idx < 0 ? "" : p.substring(0, idx);
    }

    static String siblingPath(String p, String name) {
        return childPath(parentPath(p), name);
    }

    // =================== spike 观测：记录真正被消费的路径（验证后可移除）===================

    /** 被实际读取（read/readBytes）过的包内路径，按首次读取顺序。 */
    static final Set<String> served = Collections.synchronizedSet(new LinkedHashSet<>());

    public static Set<String> servedPaths() {
        synchronized (served) {
            return new LinkedHashSet<>(served);
        }
    }
}
