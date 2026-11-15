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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动期安装<b>分卷</b>资源包包装（{@link PakBootstrap} 的多卷 + 私有 key + 三态开关版）。
 *
 * <p>必须在任何 {@code Gdx.files.internal(...)} 资源加载之前调用（App {@code create()}
 * 的第一行）。读取分卷索引（{@link VolumePakConfig#indexPath()}，由打包任务产出），
 * 逐卷解密后合并为 {@link CompositeResourcePack}，再包装 {@code Gdx.files}。</p>
 *
 * <h3>资源模式（策略注入）</h3>
 * <p>文件方式由 {@link VolumePakConfig#modeResolver()} 决定（使用方传入实现）：</p>
 * <ul>
 *   <li>{@link ResourceMode#LOOSE}——强制散包：跳过 pak 安装，即使 assets 下有索引也走明文
 *       磁盘（<b>开发态</b>：改散资源立即生效，不必重打包）；</li>
 *   <li>{@link ResourceMode#PAK}——强制 pak：探测不到索引/分卷时回退散包并 warn；</li>
 *   <li>{@link ResourceMode#AUTO}——探测到索引即 pak，否则明文磁盘（默认）。</li>
 * </ul>
 *
 * <p>分发方式：把打包目录下所有文件（各分卷 {@code *.pak} + 索引）放进 assets/（或 jar
 * classpath）。索引或全部分卷缺失时优雅跳过，资源照常走明文磁盘——开发期零成本回退。</p>
 *
 * <p>与 {@link PakBootstrap} 的区别：分卷加载 + key/索引/开关全部可配（游戏私有 key 场景）。
 * 迁自 cwwg9 的 {@code Cwwg9PakBootstrap}（2026-10-20 落地，2026-11-16 上移参数化）。</p>
 *
 * @author authorZhao
 * @since 0.3.7
 */
@Slf4j
public final class VolumePakBootstrap {

    private VolumePakBootstrap() {}

    /** 读索引 → 逐卷解密 → 合并 → 包装 Gdx.files；缺失即跳过（明文回退），异常永不炸启动。 */
    public static void init(VolumePakConfig config) {
        String tag = config.logTag();
        try {
            doInit(config, tag);
        } catch (Exception e) {
            log.warn("[{}] VolumePakBootstrap: pak 加载失败，回退明文磁盘", tag, e);
        }
    }

    private static void doInit(VolumePakConfig config, String tag) {
        ResourceMode mode = resolveMode(config, tag);
        if (mode == ResourceMode.LOOSE) {
            log.info("[{}] VolumePakBootstrap: 资源模式 LOOSE（强制散包，跳过 pak 安装）", tag);
            return;
        }

        var real = Gdx.files;
        if (real == null) {
            log.warn("[{}] VolumePakBootstrap: Gdx.files 未就绪，跳过", tag);
            return;
        }
        String indexPath = config.indexPath();
        FileHandle index = real.internal(indexPath);
        if (!index.exists()) {
            if (mode == ResourceMode.PAK) {
                log.warn("[{}] VolumePakBootstrap: 资源模式 PAK 但未找到 {}，回退明文磁盘", tag, indexPath);
            } else {
                log.info("[{}] VolumePakBootstrap: 未找到 {}，资源走明文磁盘（发布构建把 pak 分卷+索引放进 assets/ 或 jar）", tag, indexPath);
            }
            return;
        }

        PakCipher cipher = config.cipher() != null
                ? config.cipher()
                : new ChaCha20Cipher(PakKeys.KEY);
        List<ResourcePack> packs = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String line : index.readString("UTF-8").split("\n")) {
            String vol = line.trim();
            if (vol.isEmpty() || vol.startsWith("#")) continue;
            FileHandle fh = real.internal(vol);
            if (!fh.exists()) {
                missing.add(vol);
                continue;
            }
            packs.add(FileResourcePack.open(fh.readBytes(), cipher));
        }
        if (!missing.isEmpty()) {
            log.warn("[{}] VolumePakBootstrap: {} 个分卷缺失被跳过: {}", tag, missing.size(), missing);
        }
        if (packs.isEmpty()) {
            log.warn("[{}] VolumePakBootstrap: 没有可用分卷，回退明文磁盘", tag);
            return;
        }
        CompositeResourcePack pack = new CompositeResourcePack(packs);
        Gdx.files = new PakFiles(real, pack);
        log.info("[{}] VolumePakBootstrap: 已加载 {} 个分卷 / {} 个资源，Gdx.files 已包装",
                tag, pack.volumeCount(), pack.size());
    }

    private static ResourceMode resolveMode(VolumePakConfig config, String tag) {
        ResourceModeResolver resolver = config.modeResolver();
        ResourceMode mode = resolver == null ? ResourceMode.AUTO : resolver.resolve();
        return mode == null ? ResourceMode.AUTO : mode;
    }
}
