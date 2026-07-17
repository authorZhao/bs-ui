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
 * <p><b>P1 spike 行为</b>：从真实 {@code Gdx.files} 枚举 {@code com/git/bs/ui/skin} 目录，
 * 把每个文件读进 {@link MemoryResourcePack}（明文，模拟 pak 已解密加载），再包装 {@code Gdx.files}。
 * P3 会改成读取加密 {@code assets.pak}、解密后建包。</p>
 *
 * <p>开关：默认关闭，避免影响正常开发；验证时加 {@code -Dbs.pak.spike=true} 启用。</p>
 *
 * @author authorZhao
 * @since 2026-07-17
 */
@Slf4j
public final class PakBootstrap {

    /** P1 spike 装载的资源根目录（classpath 风格）。 */
    private static final String SKIN_CP = "com/git/bs/ui/skin";

    /**
     * P1 spike：skin 已知资源清单。
     * <p>lwjgl3 下 {@code internal(dir).list()} 对 classpath 目录不可靠（返回空），
     * 改用已知清单逐个 {@code internal(path).readBytes()}。真实 pak 打包器有自己的索引，
     * 这里用 skin 已知列表模拟。TTF/chinese.txt 烘焙路径不用，略。</p>
     */
    private static final String[] SKIN_FILES = {
            "bs-admin.atlas", "bs-admin.json", "bs-admin.png",
            "bs-dark.atlas", "bs-dark.json", "bs-dark.png",
            "bs-light.atlas", "bs-light.json", "bs-light.png",
            "default-font.fnt", "default-font_0.png", "default-font_1.png", "default-font_2.png",
            "font-lg.fnt", "font-lg_0.png", "font-lg_1.png", "font-lg_2.png", "font-lg_3.png", "font-lg_4.png",
            "font-md.fnt", "font-md_0.png", "font-md_1.png", "font-md_2.png",
            "font-sm.fnt", "font-sm_0.png", "font-sm_1.png",
            "font-xl.fnt", "font-xl_0.png", "font-xl_1.png", "font-xl_2.png", "font-xl_3.png",
            "font-xl_4.png", "font-xl_5.png", "font-xl_6.png", "font-xl_7.png", "font-xl_8.png",
    };

    private PakBootstrap() {}

    public static void init() {
        String flag = System.getProperty("bs.pak.spike");
        if (flag == null || flag.equalsIgnoreCase("false")) {
            return; // 默认关；验证时加 -Dbs.pak.spike=true（或 =exit 跑完自动退出）
        }
        Files real = Gdx.files;
        if (real == null) {
            log.warn("PakBootstrap[spike]: Gdx.files 未就绪，跳过");
            return;
        }

        MemoryResourcePack pack = new MemoryResourcePack();
        int n = 0;
        long bytes = 0;
        for (String name : SKIN_FILES) {
            String path = SKIN_CP + "/" + name;
            FileHandle fh = real.internal(path);
            if (!fh.exists()) continue; // 缺失则跳过（如 default-font 未生成）
            byte[] data = fh.readBytes();
            pack.put(path, data);
            n++;
            bytes += data.length;
        }

        Gdx.files = new PakFiles(real, pack);
        log.info("PakBootstrap[spike]: 装载 {} 个资源 ({} 字节)，已包装 Gdx.files", n, bytes);
        if (n == 0) {
            log.warn("PakBootstrap[spike]: 内存表为空——skin 文件都读不到？检查 classpath。");
        }
    }
}
