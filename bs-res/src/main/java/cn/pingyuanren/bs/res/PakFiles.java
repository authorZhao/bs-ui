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
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;

/**
 * {@link Files} 包装层：命中资源包的路径返回 {@link PakFileHandle}（走包内字节），
 * 其余委派给原始平台 {@code Files}（读真实磁盘/HTTP）。
 *
 * <p>由 {@link PakBootstrap#init()} 在启动期 {@code Gdx.files = new PakFiles(原 files, 包)} 安装。
 * 之后的 {@code Gdx.files.internal(path)} 对资源路径透明地走解密路径，对非资源路径无感知。</p>
 *
 * @author authorZhao
 * @since 2026-07-17
 */
public final class PakFiles implements Files {

    private final Files delegate;
    private final ResourcePack pack;

    public PakFiles(Files delegate, ResourcePack pack) {
        this.delegate = delegate;
        this.pack = pack;
    }

    /** 原始平台 Files（未包装前）。 */
    public Files delegate() {
        return delegate;
    }

    @Override
    public FileHandle internal(String path) {
        if (pack.has(path)) {
            return new PakFileHandle(path, pack, delegate.internal(path));
        }
        return delegate.internal(path);
    }

    // ---- 其余全部委派（资源只经 internal() 加载，classpath/external/absolute/local 保持原样）----

    @Override
    public FileHandle getFileHandle(String path, FileType type) {
        // BitmapFont 加载页贴图走的就是 getFileHandle(path, Internal)（见 libGDX BitmapFont
        // 「fontFile != null」分支）。不拦截这里，字体 PNG 页会绕过 pak 从磁盘加载——
        // P3 加密后磁盘无明文，字体会 404。故 Internal 类型命中 pak 的也要走 PakFileHandle。
        if (type == FileType.Internal && pack.has(path)) {
            return new PakFileHandle(path, pack, delegate.getFileHandle(path, type));
        }
        return delegate.getFileHandle(path, type);
    }

    @Override
    public FileHandle classpath(String path) {
        if (pack.has(path)) {
            return new PakFileHandle(path, pack, delegate.classpath(path));
        }
        return delegate.classpath(path);
    }

    @Override
    public FileHandle external(String path) {
        return delegate.external(path);
    }

    @Override
    public FileHandle absolute(String path) {
        return delegate.absolute(path);
    }

    @Override
    public FileHandle local(String path) {
        return delegate.local(path);
    }

    @Override
    public String getExternalStoragePath() {
        return delegate.getExternalStoragePath();
    }

    @Override
    public boolean isExternalStorageAvailable() {
        return delegate.isExternalStorageAvailable();
    }

    @Override
    public String getLocalStoragePath() {
        return delegate.getLocalStoragePath();
    }

    @Override
    public boolean isLocalStorageAvailable() {
        return delegate.isLocalStorageAvailable();
    }
}
