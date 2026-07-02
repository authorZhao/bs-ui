package com.git.teavm;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.badlogic.gdx.files.FileHandle;
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
        for (String arg : args) {
            if ("debug".equals(arg)) debug = true;
            else if ("run".equals(arg)) startJetty = true;
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
        getAssetFileHandles().forEach(teaCompiler::addAssets);
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


    private static List<AssetFileHandle> getAssetFileHandles() {

        var pre = "com/git/bs/ui/skin/";
        var files = List.of("bs-admin.atlas",
                "bs-admin.json",
                "bs-admin.png",
                "bs-dark.atlas",
                "bs-dark.json",
                "bs-dark.png",
                "bs-light.atlas",
                "bs-light.json",
                "bs-light.png",
                "default-font.fnt",
                "default-font_0.png",
                "default-font_1.png",
                "default-font_2.png",
                "font-lg.fnt",
                "font-lg_0.png",
                "font-lg_1.png",
                "font-lg_2.png",
                "font-lg_3.png",
                "font-lg_4.png",
                "font-md.fnt",
                "font-md_0.png",
                "font-md_1.png",
                "font-md_2.png",
                "font-sm.fnt",
                "font-sm_0.png",
                "font-sm_1.png",
                "font-xl.fnt",
                "font-xl_0.png",
                "font-xl_1.png",
                "font-xl_2.png",
                "font-xl_3.png",
                "font-xl_4.png",
                "font-xl_5.png",
                "font-xl_6.png",
                "font-xl_7.png",
                "font-xl_8.png");


        var list1 = files.stream().map(TeaVMBuilder::classPathHandle).collect(Collectors.toList());


        var list2 = List.of((new AssetFileHandle("../assets"))
                , (new AssetFileHandle("com/git/bs/ui/icons/bootstrap-icons.atlas", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/icons/bootstrap-icons.png", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/icons/bootstrap-icons2.png", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/icons/bootstrap-icon3.png", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/chinese.txt", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/LXGWWenKaiMonoLite-Light.ttf", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/LXGWWenKaiScreen.ttf", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("bs/test/img", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/light.json", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/light.atlas", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/light.png", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/dark.json", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/dark.atlas", com.badlogic.gdx.Files.FileType.Classpath))
                , (new AssetFileHandle("com/git/bs/ui/skin/dark.png", com.badlogic.gdx.Files.FileType.Classpath))

        );
        list1.addAll(list2);
        return list1;
    }

    private static AssetFileHandle classPathHandle(String path) {
        return new AssetFileHandle(path, com.badlogic.gdx.Files.FileType.Classpath);
    }

    ;
}
