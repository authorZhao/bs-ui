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

import java.nio.charset.StandardCharsets;

/**
 * P1 spike：PakFileHandle 表面确定性检查（不依赖 GL / Gdx.files）。
 *
 * <p>用 stub Files 包出 PakFiles，验证命中路径返回 PakFileHandle、读内容正确、
 * sibling/child/parent 路径算术正确且仍为 PakFileHandle、未命中路径委派给 stub。
 * 覆盖 spike S2 的 FileHandle 表面问题，确定性可跑。</p>
 *
 * <p>运行：{@code ./gradlew :core:pakSurfaceCheck}。</p>
 *
 * @author authorZhao
 * @since 2026-07-17
 */
public final class PakSurfaceCheck {

    private static int pass = 0;

    public static void main(String[] args) {
        MemoryResourcePack pack = new MemoryResourcePack();
        pack.put("cn/pingyuanren/bs/ui/skin/bs-dark.json", "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        pack.put("cn/pingyuanren/bs/ui/skin/bs-dark.atlas", "atlas\n".getBytes(StandardCharsets.UTF_8));
        pack.put("cn/pingyuanren/bs/ui/skin/bs-dark.png", new byte[]{1, 2, 3});

        RecordingFiles stub = new RecordingFiles();
        PakFiles files = new PakFiles(stub, pack);

        // 1. 命中：internal 返回 PakFileHandle
        FileHandle json = files.internal("cn/pingyuanren/bs/ui/skin/bs-dark.json");
        check(json instanceof PakFileHandle, "internal 命中 -> PakFileHandle");
        check(json.exists(), "exists() == true");
        check("{\"a\":1}".equals(json.readString()), "readString() 内容正确");
        check(json.length() == 7, "length() == 7（实际 " + json.length() + "）");
        check(json.readBytes().length == 7, "readBytes().length == 7");

        // 2. sibling 链：json -> atlas -> png（模拟 Skin/TextureAtlas 的解析路径）
        FileHandle atlas = json.sibling("bs-dark.atlas");
        check(atlas instanceof PakFileHandle, "sibling -> PakFileHandle");
        check("cn/pingyuanren/bs/ui/skin/bs-dark.atlas".equals(atlas.path()),
                "sibling 路径 = " + atlas.path());
        check(atlas.exists(), "sibling exists()");
        check("atlas\n".equals(atlas.readString()), "sibling readString() 正确");

        FileHandle png = atlas.sibling("bs-dark.png");
        check("cn/pingyuanren/bs/ui/skin/bs-dark.png".equals(png.path()), "png 路径 = " + png.path());
        check(png.readBytes().length == 3, "png readBytes().length == 3");

        // 3. child / parent 算术
        check("cn/pingyuanren/bs/ui/skin".equals(json.parent().path()),
                "parent() = " + json.parent().path());
        check("cn/pingyuanren/bs/ui/skin/bs-dark.json/sub.txt".equals(json.child("sub.txt").path()),
                "child() = " + json.child("sub.txt").path());

        // 4. 二级 sibling：json -> font-sm.fnt（模拟 skin json 引用字体）
        FileHandle fnt = json.sibling("font-sm.fnt");
        check(fnt instanceof PakFileHandle, "font sibling -> PakFileHandle");
        check(!fnt.exists(), "font-sm.fnt 不在表里 -> exists()==false（按预期）");

        // 5. 未命中路径 -> 委派给 stub
        stub.reset();
        FileHandle miss = files.internal("cn/pingyuanren/bs/ui/skin/NOT_EXIST.txt");
        check("cn/pingyuanren/bs/ui/skin/NOT_EXIST.txt".equals(stub.lastInternal),
                "未命中路径委派给 stub：lastInternal=" + stub.lastInternal);
        check(!(miss instanceof PakFileHandle), "委派返回的不是 PakFileHandle");

        // 6. external/absolute 等不经 pak
        stub.reset();
        files.external("/tmp/x");
        check(stub.lastExternal != null, "external() 走 stub");

        // 7. getFileHandle(Internal) 命中也必须走 PakFileHandle
        //    —— BitmapFont 加载字体页贴图走的就是这条（fontFile!=null 分支），不拦会绕过 pak
        FileHandle viaGet = files.getFileHandle("cn/pingyuanren/bs/ui/skin/bs-dark.json", FileType.Internal);
        check(viaGet instanceof PakFileHandle, "getFileHandle(Internal) 命中 -> PakFileHandle");
        check("{\"a\":1}".equals(viaGet.readString()), "getFileHandle(Internal) 内容正确");

        // getFileHandle(External) 不拦截（仅 Internal 命中 pak）
        FileHandle viaExt = files.getFileHandle("/tmp/x.png", FileType.External);
        check(!(viaExt instanceof PakFileHandle), "getFileHandle(External) 不走 pak");

        System.out.println();
        System.out.println("PakSurfaceCheck: 全部通过（" + pass + " 项）");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError("FAIL: " + msg);
        }
        pass++;
        System.out.println("  ok  " + msg);
    }

    /** 记录委派调用的 stub Files。 */
    static final class RecordingFiles implements Files {
        String lastInternal;
        String lastExternal;

        void reset() {
            lastInternal = null;
            lastExternal = null;
        }

        @Override public FileHandle internal(String path) {
            lastInternal = path;
            return new StubHandle(path,FileType.Internal);
        }

        @Override public FileHandle getFileHandle(String path, FileType type) {
            return new StubHandle(path,type);
        }

        @Override public FileHandle classpath(String path) {
            return new StubHandle(path,FileType.Classpath);
        }

        @Override public FileHandle external(String path) {
            lastExternal = path;
            return new StubHandle(path,FileType.External);
        }

        @Override public FileHandle absolute(String path) {
            return new StubHandle(path,FileType.Absolute);
        }

        @Override public FileHandle local(String path) {
            return new StubHandle(path,FileType.Local);
        }

        @Override public String getExternalStoragePath() {
            return "";
        }

        @Override public boolean isExternalStorageAvailable() {
            return false;
        }

        @Override public String getLocalStoragePath() {
            return "";
        }

        @Override public boolean isLocalStorageAvailable() {
            return false;
        }
    }

    /**
     * 能用 protected {@code FileHandle(String,FileType)} 构造器的最小子类，
     * 供 {@link RecordingFiles} 在委派时返回（stub 不需要真实磁盘文件）。
     */
    static final class StubHandle extends FileHandle {
        StubHandle(String path, FileType type) {
            super(path, type);
        }
    }

    private PakSurfaceCheck() {}
}
