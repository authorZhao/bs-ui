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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分卷启动器：三态开关 / 索引解析 / 缺卷跳过 / 明文回退。
 * Gdx.files 换成内存 FakeGdxFiles，测试后还原。
 *
 * @author authorZhao
 * @since 0.3.7
 */
class VolumePakBootstrapTest {

    private FakeGdxFiles fake;
    private com.badlogic.gdx.Files original;

    @BeforeEach
    void installFakeFiles() {
        original = Gdx.files;
        fake = new FakeGdxFiles();
        Gdx.files = fake;
    }

    @AfterEach
    void restoreFiles() {
        Gdx.files = original;
    }

    private void putVolume(String name, String... pathAndSingleContent) {
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
        for (int i = 0; i < pathAndSingleContent.length; i += 2) {
            entries.put(pathAndSingleContent[i],
                    pathAndSingleContent[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        byte[] salt = new byte[16];
        fake.put(name, PakWriter.write(entries, IdentityCipher.INSTANCE, salt));
    }

    @Test
    void autoLoadsVolumesAndWrapsFiles() {
        putVolume("vol1.pak", "particle/lizi_19.png", "fx1");
        putVolume("vol2.pak", "skin/bs.json", "skin1");
        fake.putText("paks.txt", "# 注释行\nvol1.pak\nvol2.pak\n");

        VolumePakBootstrap.init(VolumePakConfig.builder()
                .cipher(IdentityCipher.INSTANCE)
                .build()); // 未配 resolver = 恒 AUTO

        assertInstanceOf(PakFiles.class, Gdx.files);
        assertEquals("fx1", new String(Gdx.files.internal("particle/lizi_19.png").readBytes(),
                StandardCharsets.UTF_8));
        // 目录句柄接进 pak（目录感知），child 拼全路径后命中
        FileHandle dir = Gdx.files.internal("particle");
        assertInstanceOf(PakFileHandle.class, dir);
        assertTrue(dir.child("lizi_19.png").exists());
        // 不在 pak 的路径透传
        assertFalse(Gdx.files.internal("saves/slot1.json").exists());
    }

    @Test
    void looseModeSkipsInstall() {
        putVolume("vol1.pak", "a.png", "1");
        fake.putText("paks.txt", "vol1.pak\n");

        VolumePakBootstrap.init(VolumePakConfig.builder()
                .cipher(IdentityCipher.INSTANCE)
                .modeResolver(() -> ResourceMode.LOOSE)
                .build());

        assertSame(fake, Gdx.files, "LOOSE 应跳过 pak 安装");
    }

    @Test
    void pakModeWithoutIndexFallsBackToLoose() {
        // 有分卷但没有索引
        putVolume("vol1.pak", "a.png", "1");

        VolumePakBootstrap.init(VolumePakConfig.builder()
                .cipher(IdentityCipher.INSTANCE)
                .modeResolver(() -> ResourceMode.PAK)
                .build());

        assertSame(fake, Gdx.files, "PAK 探测不到索引应回退明文磁盘");
    }

    @Test
    void autoWithoutIndexStaysLoose() {
        VolumePakBootstrap.init(VolumePakConfig.builder()
                .cipher(IdentityCipher.INSTANCE)
                .build());
        assertSame(fake, Gdx.files, "AUTO 无索引应走明文磁盘");
    }

    @Test
    void missingVolumesAreSkipped() {
        putVolume("vol1.pak", "a.png", "1");
        fake.putText("paks.txt", "vol1.pak\nghost.pak\n");

        VolumePakBootstrap.init(VolumePakConfig.builder()
                .cipher(IdentityCipher.INSTANCE)
                .build());

        assertInstanceOf(PakFiles.class, Gdx.files, "缺 1 卷仍应装上其余卷");
        assertEquals("1", new String(Gdx.files.internal("a.png").readBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void allVolumesMissingFallsBackToLoose() {
        fake.putText("paks.txt", "ghost.pak\n");

        VolumePakBootstrap.init(VolumePakConfig.builder()
                .cipher(IdentityCipher.INSTANCE)
                .build());

        assertSame(fake, Gdx.files, "全部卷缺失应回退明文磁盘");
    }

    @Test
    void sysPropsResolverReadsPropEnvAuto() {
        String prop = "bs.ui.test.assets.mode";
        var resolver = new SysPropsResourceModeResolver(prop, null);
        System.clearProperty(prop);
        assertEquals(ResourceMode.AUTO, resolver.resolve());
        System.setProperty(prop, "loose");
        assertEquals(ResourceMode.LOOSE, resolver.resolve());
        System.setProperty(prop, "PAK");
        assertEquals(ResourceMode.PAK, resolver.resolve());
        System.setProperty(prop, "  auto  ");
        assertEquals(ResourceMode.AUTO, resolver.resolve());
        System.setProperty(prop, "垃圾值");
        assertEquals(ResourceMode.AUTO, resolver.resolve(), "未知值按 AUTO");
        System.clearProperty(prop);
    }
}
