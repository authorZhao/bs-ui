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
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动期安装资源包包装。必须在任何 {@code Gdx.files.internal(...)} 资源加载之前调用
 * （早于 {@code BsUI.init()} / {@code BsSkinLoader.loadAllThemes()}）。
 *
 * <p><b>行为</b>：从 classpath 读 {@code assets.pak}（由 {@link PakPacker}/packResources 任务产出），
 * 用 {@link FileResourcePack} 解析（默认 ChaCha20 + PakKeys.KEY 解密），再包装 {@code Gdx.files}。
 * {@code assets.pak} 不在 classpath 时优雅跳过（资源走明文磁盘，方便开发）。</p>
 *
 * @author authorZhao
 * @since 2026-07-17
 */
@Slf4j
public final class PakBootstrap {

    /** 打包器产出的资源包在 classpath 下的名字（见 {@link PakPacker} / packResources 任务）。 */
    private static final String PAK_PATH = "assets.pak";

    private PakBootstrap() {}

    public static void init() {
        Files real = Gdx.files;
        if (real == null) {
            log.warn("PakBootstrap: Gdx.files 未就绪，跳过");
            return;
        }

        FileHandle pakFile = real.internal(PAK_PATH);
        if (!pakFile.exists()) {
            log.warn("PakBootstrap: classpath 下没有 {}，跳过（需先跑 packResources 任务）；资源走明文磁盘。",
                    PAK_PATH);
            return;
        }
        byte[] pakBytes = pakFile.readBytes();
        FileResourcePack pack = FileResourcePack.open(pakBytes); // 默认 ChaCha20(PakKeys.KEY)
        Gdx.files = new PakFiles(real, pack);
        log.info("PakBootstrap: 从 {} 加载 {} 个资源（pak {} 字节），已包装 Gdx.files",
                PAK_PATH, pack.size(), pakBytes.length);
    }
}
