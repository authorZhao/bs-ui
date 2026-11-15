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

import java.util.List;

/**
 * 多分卷 {@link ResourcePack} 合并视图：按注册顺序查找，第一个命中卷返回。
 *
 * <p>assets 按一级目录拆成多个 {@code *.pak} 分卷（每卷 &lt; 100MB，普通 git 可推、
 * 免 LFS），运行时把所有卷合并成一个视图交给 {@link PakFiles} 包装。迁自 cwwg9
 * （2026-10-20 落地，2026-11-16 上移 bs-ui）。</p>
 *
 * @author authorZhao
 * @since 0.3.7
 */
public final class CompositeResourcePack implements ResourcePack {

    private final List<ResourcePack> packs;
    private final int entryCount;

    public CompositeResourcePack(List<ResourcePack> packs) {
        this.packs = List.copyOf(packs);
        // ResourcePack 接口无条目数；当前分卷都是 FileResourcePack，特判统计（嵌套 Composite 会计 0，不支持的用法）
        int count = 0;
        for (ResourcePack p : packs) {
            if (p instanceof FileResourcePack f) count += f.size();
        }
        this.entryCount = count;
    }

    @Override
    public boolean has(String logicalPath) {
        for (ResourcePack p : packs) {
            if (p.has(logicalPath)) return true;
        }
        return false;
    }

    @Override
    public byte[] read(String logicalPath) {
        for (ResourcePack p : packs) {
            if (p.has(logicalPath)) return p.read(logicalPath);
        }
        return null;
    }

    @Override
    public long length(String logicalPath) {
        for (ResourcePack p : packs) {
            if (p.has(logicalPath)) return p.length(logicalPath);
        }
        return 0;
    }

    @Override
    public boolean hasDir(String dir) {
        for (ResourcePack p : packs) {
            if (p.hasDir(dir)) return true;
        }
        return false;
    }

    /** 全部卷条目数之和。 */
    public int size() {
        return entryCount;
    }

    /** 分卷数。 */
    public int volumeCount() {
        return packs.size();
    }
}
