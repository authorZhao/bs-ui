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

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import lombok.extern.slf4j.Slf4j;

/**
 * 启动期安装资源包包装。必须在任何 {@code Gdx.files.internal(...)} 资源加载之前调用
 * （早于 {@code BsUI.init()} / {@code BsSkinLoader.loadAllThemes()}）。
 *
 * <p><b>行为</b>：从 classpath 读 {@code assets.pak}（由 {@link PakPacker}/packResources 任务产出），
 * 用 {@link FileResourcePack} 解析，再包装 {@code Gdx.files}。P2 用 {@link IdentityCipher}（明文），
 * P3 换 ChaCha20。{@code assets.pak} 不在 classpath 时优雅跳过（资源走明文磁盘，方便开发）。</p>
 *
 * <p>开关：默认关闭，避免影响正常开发；验证时加 {@code -Dbs.pak.spike=true} 启用。</p>
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
        FileResourcePack pack = FileResourcePack.open(pakBytes); // P2 identity cipher；P3 传 ChaCha20
        Gdx.files = new PakFiles(real, pack);
        log.info("PakBootstrap: 从 {} 加载 {} 个资源（pak {} 字节），已包装 Gdx.files",
                PAK_PATH, pack.size(), pakBytes.length);
    }
}
