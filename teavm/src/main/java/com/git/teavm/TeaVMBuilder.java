package com.git.teavm;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.git.bs.demo.BsControlsTestScreen;
import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.vm.TeaVMOptimizationLevel;

/**
 * Builds the TeaVM/HTML application.
 */
public class TeaVMBuilder {
    public static void main(String[] args) throws IOException {

        // Typically set by the Gradle task, but can also be set here or with the command-line arg "debug"
        boolean debug = false;
        // Typically set by the Gradle task, but can also be set here or with the command-line arg "run"
        boolean startJetty = false;
        // releasePak 任务传 "pak"：资源打进 assets.pak（发布线上）；不传则开发/原 buildRelease 走散列资源
        boolean pak = false;
        for (String arg : args) {
            if ("debug".equals(arg)) debug = true;
            else if ("run".equals(arg)) startJetty = true;
            else if ("pak".equals(arg)) pak = true;
        }

        WebBackend webBackend = new WebBackend();
        //webBackend.tool.setTargetType(TeaVMTargetType.JAVASCRIPT);
        TeaCompiler teaCompiler = new TeaCompiler(
                webBackend
                        .setHtmlWidth(BsControlsTestScreen.WIN_W) /* Change this to fit your game's requirements. */
                        .setHtmlHeight(BsControlsTestScreen.WIN_H) /* Change this to fit your game's requirements. */
                        .setHtmlTitle("bs-demo")
                        .setWebAssembly(true) /* Uncomment this line to use WASM output instead of JavaScript output. */
                        .setStartJettyAfterBuild(startJetty)
                        .setJettyPort(8080)
        );
        getAssetFileHandles(pak).forEach(teaCompiler::addAssets);
        getReflectionClasses().forEach(teaCompiler::addReflectionClass);
        teaCompiler
                .setMaxHeapSize(1024 * 1024 * 1024)
                .setOptimizationLevel(debug ? TeaVMOptimizationLevel.SIMPLE : TeaVMOptimizationLevel.ADVANCED)
                .setMainClass(TeaVMLauncher.class.getName())
                .setObfuscated(!debug)
                .setDebugInformationGenerated(debug)
                .setSourceMapsFileGenerated(debug)
                .setSourceFilePolicy(TeaVMSourceFilePolicy.COPY)
                .addSourceFileProvider(new DirectorySourceFileProvider(new File("../core/src/main/java/")))
                .build(new File("build/dist"));


    }

    private static List<String> getReflectionClasses() {
        return List.of(
                "com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator",
                "com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator$FreeTypeFontParameter",
                "com.git.teavm.platform.TeaVmPlatform");
    }


    private static List<AssetFileHandle> getAssetFileHandles(boolean pak) {
        // 外部 assets 目录（运行时通过 Gdx.files.internal("...") 加载的路径）
        var list = new java.util.ArrayList<AssetFileHandle>();
        list.add(new AssetFileHandle("../assets"));
        list.add(new AssetFileHandle("bs/test/img", com.badlogic.gdx.Files.FileType.Classpath));

        if (pak) {
            // 发布线上：skin/icons/emoji/core-i18n/demo-i18n 全打进 assets.pak（单文件 HTTP + P3 加密）。
            // 运行时由 PakBootstrap（WinSettingsApp.create() 已接）加载；gdx-teavm 预加载 assets.pak 后，
            // internal("...") 经 PakFiles 透明走 pak。assets.pak 由 releasePak 任务的 packResources 产出。
            // skin/icons/emoji/core-i18n/demo-i18n 全在 assets.pak 里。assets.pak 不走 addAssets
            //（gdx-teavm copyDirectory 对单文件直接跳过），由 releasePak 任务的 doLast 直接拷进
            // build/dist/webapp/assets/ 并在 preload.txt 补一行——见 teavm/build.gradle。
        } else {
            // 开发 / 原 buildRelease：资源原样散列（免打包，改了即生效）
            var skinPre = "com/git/bs/ui/skin/";
            list.add(cp(skinPre + "bs-admin.atlas"));
            list.add(cp(skinPre + "bs-admin.json"));
            list.add(cp(skinPre + "bs-admin.png"));
            list.add(cp(skinPre + "bs-dark.atlas"));
            list.add(cp(skinPre + "bs-dark.json"));
            list.add(cp(skinPre + "bs-dark.png"));
            list.add(cp(skinPre + "bs-light.atlas"));
            list.add(cp(skinPre + "bs-light.json"));
            list.add(cp(skinPre + "bs-light.png"));
            list.add(cp(skinPre + "default-font.fnt"));
            list.add(cp(skinPre + "default-font_0.png"));
            list.add(cp(skinPre + "default-font_1.png"));
            list.add(cp(skinPre + "default-font_2.png"));
            list.add(cp(skinPre + "font-lg.fnt"));
            list.add(cp(skinPre + "font-lg_0.png"));
            list.add(cp(skinPre + "font-lg_1.png"));
            list.add(cp(skinPre + "font-lg_2.png"));
            list.add(cp(skinPre + "font-lg_3.png"));
            list.add(cp(skinPre + "font-lg_4.png"));
            list.add(cp(skinPre + "font-md.fnt"));
            list.add(cp(skinPre + "font-md_0.png"));
            list.add(cp(skinPre + "font-md_1.png"));
            list.add(cp(skinPre + "font-md_2.png"));
            list.add(cp(skinPre + "font-sm.fnt"));
            list.add(cp(skinPre + "font-sm_0.png"));
            list.add(cp(skinPre + "font-sm_1.png"));
            list.add(cp(skinPre + "font-xl.fnt"));
            list.add(cp(skinPre + "font-xl_0.png"));
            list.add(cp(skinPre + "font-xl_1.png"));
            list.add(cp(skinPre + "font-xl_2.png"));
            list.add(cp(skinPre + "font-xl_3.png"));
            list.add(cp(skinPre + "font-xl_4.png"));
            list.add(cp(skinPre + "font-xl_5.png"));
            list.add(cp(skinPre + "font-xl_6.png"));
            list.add(cp(skinPre + "font-xl_7.png"));
            list.add(cp(skinPre + "font-xl_8.png"));

            // icons：bootstrap-icons 图标集（atlas + 3 张 png）
            var iconPre = "com/git/bs/ui/icons/";
            list.add(cp(iconPre + "bootstrap-icons.atlas"));
            list.add(cp(iconPre + "bootstrap-icons.png"));
            list.add(cp(iconPre + "bootstrap-icons2.png"));
            list.add(cp(iconPre + "bootstrap-icons3.png"));

            // emoji：彩色 emoji + 头像图集（BsEmoji 加载）
            var emojiPre = "com/git/bs/ui/emoji/";
            list.add(cp(emojiPre + "emoji.atlas"));
            list.add(cp(emojiPre + "emoji.png"));
            list.add(cp(emojiPre + "pack2.png"));
            list.add(cp(emojiPre + "pack3.png"));
            list.add(cp(emojiPre + "pack4.png"));
            list.add(cp(emojiPre + "pack5.png"));
            list.add(cp(emojiPre + "head_emoji.atlas"));
            list.add(cp(emojiPre + "head_emoji.png"));

            // i18n：core 通用翻译包（properties，3 个 locale）
            var i18nPre = "com/git/bs/i18n/";
            list.add(cp(i18nPre + "zh_cn.properties"));
            list.add(cp(i18nPre + "en_us.properties"));
            list.add(cp(i18nPre + "ja_jp.properties"));

            // winsettings 业务翻译包（properties，2 个 locale）
            var demoI18nPre = "com/git/bs/demo/i18n/";
            list.add(cp(demoI18nPre + "zh_cn.properties"));
            list.add(cp(demoI18nPre + "en_us.properties"));
        }

        return list;
    }

    /** classpath 资源快捷构造。 */
    private static AssetFileHandle cp(String path) {
        return new AssetFileHandle(path, com.badlogic.gdx.Files.FileType.Classpath);
    }
}
