package com.git.bs.res;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PakFileHandle 表面 + 导航 + getFileHandle 拦截回归。
 * 对应手动检查 main {@code PakSurfaceCheck}；CI/gradle test 自动版。
 */
class PakFileHandleTest {

    @Test
    void surfaceSiblingAndInterception() {
        var pack = new MemoryResourcePack();
        pack.put("com/git/bs/ui/skin/bs-dark.json", "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        pack.put("com/git/bs/ui/skin/bs-dark.atlas", "atlas\n".getBytes(StandardCharsets.UTF_8));
        pack.put("com/git/bs/ui/skin/bs-dark.png", new byte[]{1, 2, 3});

        PakFiles files = new PakFiles(new StubFiles(), pack);

        // internal 命中 → PakFileHandle
        FileHandle json = files.internal("com/git/bs/ui/skin/bs-dark.json");
        assertTrue(json instanceof PakFileHandle);
        assertTrue(json.exists());
        assertEquals("{\"a\":1}", json.readString());
        assertEquals(7, json.length());
        assertArrayEquals("{\"a\":1}".getBytes(StandardCharsets.UTF_8), json.readBytes());

        // sibling 链（json → atlas → png）：全部仍是 PakFileHandle
        FileHandle atlas = json.sibling("bs-dark.atlas");
        assertTrue(atlas instanceof PakFileHandle);
        assertEquals("com/git/bs/ui/skin/bs-dark.atlas", atlas.path());
        assertEquals("atlas\n", atlas.readString());

        FileHandle png = atlas.sibling("bs-dark.png");
        assertEquals("com/git/bs/ui/skin/bs-dark.png", png.path());
        assertArrayEquals(new byte[]{1, 2, 3}, png.readBytes());

        // parent / child 算术
        assertEquals("com/git/bs/ui/skin", json.parent().path());
        assertEquals("com/git/bs/ui/skin/bs-dark.json/sub.txt", json.child("sub.txt").path());

        // getFileHandle(Internal) 命中也走 PakFileHandle（BitmapFont 字体页贴图走这条！）
        FileHandle viaGet = files.getFileHandle("com/git/bs/ui/skin/bs-dark.json", FileType.Internal);
        assertTrue(viaGet instanceof PakFileHandle);
        assertEquals("{\"a\":1}", viaGet.readString());

        // getFileHandle(External) 不拦截
        FileHandle viaExt = files.getFileHandle("/tmp/x.png", FileType.External);
        assertFalse(viaExt instanceof PakFileHandle);

        // 未命中 → 委派 stub（非 PakFileHandle）
        FileHandle miss = files.internal("com/git/bs/ui/skin/NOT_EXIST.txt");
        assertFalse(miss instanceof PakFileHandle);
    }

    /** stub Files：委派时返回一个最小 FileHandle（不读真实磁盘）。 */
    static final class StubFiles implements Files {
        @Override public FileHandle internal(String path) { return new StubHandle(path, FileType.Internal); }
        @Override public FileHandle getFileHandle(String path, FileType type) { return new StubHandle(path, type); }
        @Override public FileHandle classpath(String path) { return new StubHandle(path, FileType.Classpath); }
        @Override public FileHandle external(String path) { return new StubHandle(path, FileType.External); }
        @Override public FileHandle absolute(String path) { return new StubHandle(path, FileType.Absolute); }
        @Override public FileHandle local(String path) { return new StubHandle(path, FileType.Local); }
        @Override public String getExternalStoragePath() { return ""; }
        @Override public boolean isExternalStorageAvailable() { return false; }
        @Override public String getLocalStoragePath() { return ""; }
        @Override public boolean isLocalStorageAvailable() { return false; }
    }

    static final class StubHandle extends FileHandle {
        StubHandle(String path, FileType type) { super(path, type); }
    }
}
