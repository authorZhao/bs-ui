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

package cn.pingyuanren.bs.test.iconpkg;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bootstrap Icons SVG → libgdx TextureAtlas 转换工具。
 *
 * <p><b>流程</b>：</p>
 * <ol>
 *   <li>扫描输入目录所有 .svg 文件</li>
 *   <li>用 Apache Batik 把每个 SVG 渲染成 BufferedImage（指定尺寸 + 颜色）</li>
 *   <li>用 libgdx {@link PixmapPacker} 把所有 BufferedImage 打包到一张或多张 Pixmap（atlas 页）</li>
 *   <li>导出 PNG + libgdx TextureAtlas 格式（.atlas 文本文件）</li>
 * </ol>
 *
 * <p><b>参数</b>（用 {@link PackConfig} builder）：</p>
 * <ul>
 *   <li>{@code inputDir} —— SVG 输入目录</li>
 *   <li>{@code outputDir} —— 输出目录（写 bootstrap-icons.atlas + bootstrap-icons.png）</li>
 *   <li>{@code iconSize} —— 单个图标渲染尺寸（默认 32）</li>
 *   <li>{@code atlasSize} —— atlas 页大小（默认 1024）</li>
 *   <li>{@code padding} —— 图标间 padding（默认 2）</li>
 *   <li>{@code fillColor} —— 染色（"white"/"#0D6EFD"/null=保持 SVG 原色）</li>
 *   <li>{@code includeFilter} —— 只包含这些图标（按文件名，不含扩展名）；为空=全部</li>
 *   <li>{@code excludeFilter} —— 排除这些图标</li>
 * </ul>
 *
 * <p><b>用法</b>：</p>
 * <pre>{@code
 * PackConfig cfg = PackConfig.builder()
 *         .inputDir("E:/idea/workspace2/test/icons")
 *         .outputDir("assets/bs/icons")
 *         .iconSize(32)
 *         .atlasSize(1024)
 *         .fillColor("#FFFFFF")
 *         .build();
 * BootstrapIconPackager.pack(cfg, progress -> {
 *             System.out.printf("[%d/%d] %s%n", progress.done, progress.total, progress.current);
 *         });
 * }</pre>
 */
@Slf4j
public class BootstrapIconPackager {

    /**
     * 强制 Batik 使用 JDK 自带的 XML parser（com.sun.org.apache.xerces.internal.parsers.SAXParser），
     * 而不是 classpath 上抢答的 org.gjt.xpp（dom4j 间接引入）—— xpp 的 parser 不识别
     * "http://xml.org/sax/features/external-general-entities" feature，
     * 会让 Batik 抛 SAXNotRecognizedException。
     */
    static {
        try {
            // 优先用 Apache Xerces（如果有），否则用 JDK 自带
            String parser;
            try {
                Class.forName("org.apache.xerces.parsers.SAXParser");
                parser = "org.apache.xerces.parsers.SAXParser";
            } catch (ClassNotFoundException e) {
                parser = "com.sun.org.apache.xerces.internal.parsers.SAXParser";
            }
            org.apache.batik.util.XMLResourceDescriptor.setXMLParserClassName(parser);
            log.info("Batik XML parser 设为: {}", parser);
        } catch (Throwable t) {
            log.warn("设置 Batik XML parser 失败", t);
        }
    }

    /** 转换参数（builder 风格）。 */
    @Builder
    public static class PackConfig {
        /** SVG 输入目录（绝对路径或相对路径）。 */
        public String inputDir;
        /** 输出目录。 */
        public String outputDir;
        /** 单个图标渲染尺寸（像素）。默认 32。 */
        @Builder.Default public int iconSize = 32;
        /** atlas 页大小（像素，建议 2 的幂：512/1024/2048）。默认 1024。 */
        @Builder.Default public int atlasSize = 1024;
        /** 图标间 padding（像素）。默认 2。 */
        @Builder.Default public int padding = 2;
        /** 染色 hex（"white"/"#0D6EFD"/null）。null=保持 SVG 原色（Bootstrap 默认 currentColor=黑）。 */
        public String fillColor;
        /** 只包含这些图标（按文件名不含 .svg 后缀）。为空=包含全部。 */
        public List<String> includeFilter;
        /** 排除这些图标。 */
        public List<String> excludeFilter;
        /** 输出 atlas 基础名（默认 "bootstrap-icons"）。 */
        @Builder.Default public String atlasName = "bootstrap-icons";
    }

    /** 进度回调。 */
    public static class Progress {
        public final int done;
        public final int total;
        public final String current;
        public Progress(int done, int total, String current) {
            this.done = done; this.total = total; this.current = current;
        }
    }

    /**
     * 执行打包。
     * <p><b>简化版</b>：每个 SVG → 单独 PNG（写到临时目录），然后用 libgdx 内置的
     * {@link com.badlogic.gdx.tools.texturepacker.TexturePacker} 把所有 PNG 打成 atlas。
     * 这样 atlas 格式由 libgdx 自己生成，绝对正确。</p>
     *
     * @param cfg 参数
     * @param progressCb 进度回调（null=不回调）
     */
    public static void pack(PackConfig cfg, Consumer<Progress> progressCb) {
        File inDir = new File(cfg.inputDir);
        if (!inDir.isDirectory()) {
            throw new IllegalArgumentException("inputDir 不存在或不是目录: " + cfg.inputDir);
        }
        File outDir = new File(cfg.outputDir);
        outDir.mkdirs();

        // 1. 扫描 + 过滤 SVG
        File[] svgFiles = inDir.listFiles((d, n) -> n.toLowerCase().endsWith(".svg"));
        if (svgFiles == null || svgFiles.length == 0) {
            throw new IllegalStateException("未找到 SVG 文件: " + cfg.inputDir);
        }
        List<File> targets = new ArrayList<>();
        for (File f : svgFiles) {
            String name = stripSvgExt(f.getName());
            if (cfg.excludeFilter != null && cfg.excludeFilter.contains(name)) continue;
            if (cfg.includeFilter != null && !cfg.includeFilter.isEmpty()
                    && !cfg.includeFilter.contains(name)) continue;
            targets.add(f);
        }
        log.info("Bootstrap Icons packager: 共 {} 个 SVG（过滤后），开始转换", targets.size());

        // 2. 临时目录：放 SVG→PNG 转换结果（每个图标一张 PNG）
        File tmpPngDir = new File(outDir, ".tmp-png-" + System.currentTimeMillis());
        tmpPngDir.mkdirs();
        try {
            int done = 0;
            for (File svg : targets) {
                String iconName = stripSvgExt(svg.getName());
                try {
                    BufferedImage bimg = renderSvgToImage(svg, cfg.iconSize, cfg.fillColor);
                    File pngFile = new File(tmpPngDir, iconName + ".png");
                    ImageIO.write(bimg, "png", pngFile);
                } catch (Throwable t) {
                    log.warn("转换失败: {} ({})", iconName, t.getMessage());
                }
                done++;
                if (progressCb != null) {
                    progressCb.accept(new Progress(done, targets.size(), iconName));
                }
            }

            // 3. 用 libgdx TexturePacker 把临时 PNG 目录打成 atlas
            com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings settings =
                    new com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings();
            settings.maxWidth = cfg.atlasSize;
            settings.maxHeight = cfg.atlasSize;
            settings.paddingX = cfg.padding;
            settings.paddingY = cfg.padding;
            settings.duplicatePadding = true;
            settings.pot = true;  // 2 的幂尺寸
            settings.silent = true;
            settings.combineSubdirectories = true;
            settings.filterMin = Texture.TextureFilter.Linear;
            settings.filterMag = Texture.TextureFilter.Linear;

            com.badlogic.gdx.tools.texturepacker.TexturePacker tp =
                    new com.badlogic.gdx.tools.texturepacker.TexturePacker(settings);
            // 把临时 PNG 全部塞进 TexturePacker（用 BufferedImage）
            File[] pngFiles = tmpPngDir.listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
            if (pngFiles != null) {
                for (File png : pngFiles) {
                    try {
                        BufferedImage bimg = ImageIO.read(png);
                        if (bimg != null) {
                            tp.addImage(bimg, stripPngExt(png.getName()));
                        }
                    } catch (Throwable t) {
                        log.warn("加入 {} 失败: {}", png.getName(), t.getMessage());
                    }
                }
            }
            // 输出 atlas
            tp.pack(outDir, cfg.atlasName);
            log.info("✓ 打包完成: {}/{}.atlas + {}/{}.png", outDir, cfg.atlasName, outDir, cfg.atlasName);
        } finally {
            // 清理临时目录
            try {
                File[] fs = tmpPngDir.listFiles();
                if (fs != null) for (File f : fs) f.delete();
                tmpPngDir.delete();
            } catch (Throwable ignored) {}
        }
    }

    private static String stripPngExt(String name) {
        return name.replaceAll("(?i)\\.png$", "");
    }

    // ========================= Batik SVG 渲染 =========================

    /** 把单个 SVG 渲染成 BufferedImage（指定尺寸 + 可选染色）。 */
    private static BufferedImage renderSvgToImage(File svgFile, int size, String fillColor) throws Exception {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        SVGDocument doc;
        try (FileInputStream in = new FileInputStream(svgFile)) {
            doc = (SVGDocument) factory.createDocument(null, in);
        }

        // 如果指定了 fillColor，覆盖 SVG 的 fill（通过 style 属性 CSS）
        if (fillColor != null && !fillColor.isEmpty()) {
            String fillHex = fillColor.startsWith("#") ? fillColor.substring(1) : fillColor;
            String fillRgb = hexToRgb(fillHex);
            doc.getDocumentElement().setAttribute("style", "fill: " + fillRgb + ";");
        }

        // 用自定义 ImageTranscoder 直接拿到 BufferedImage
        BufferedImageTranscoder2 t = new BufferedImageTranscoder2();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);
        TranscoderInput input = new TranscoderInput(doc);
        t.transcode(input, null);
        return t.getBufferedImage();
    }

    /** 自定义 transcoder 直接输出 BufferedImage（不写到 OutputStream）。 */
    private static class BufferedImageTranscoder2 extends org.apache.batik.transcoder.image.ImageTranscoder {
        private BufferedImage img;
        @Override
        public BufferedImage createImage(int w, int h) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        @Override
        public void writeImage(BufferedImage img, org.apache.batik.transcoder.TranscoderOutput out) {
            this.img = img;
        }
        public BufferedImage getBufferedImage() { return img; }
    }

    // ========================= Pixmap 转换 =========================

    /** BufferedImage → Pixmap。 */
    private static Pixmap bufferedImageToPixmap(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        Pixmap pix = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        // 逐像素拷贝（PNGTranscoder 默认 ARGB）
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = pixels[y * w + x];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                pix.drawPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);  // Pixmap RGBA8888 同 int 编码
            }
        }
        return pix;
    }

    /** 写 Pixmap → PNG 文件（备用，TexturePacker 路径不用）。 */
    private static void writePixmapToFile(Pixmap pix, File file) throws IOException {
        com.badlogic.gdx.files.FileHandle fh = com.badlogic.gdx.Gdx.files.absolute(file.getAbsolutePath());
        com.badlogic.gdx.graphics.PixmapIO.writePNG(fh, pix);
    }

    // ========================= 工具 =========================

    private static String stripSvgExt(String name) {
        return name.replaceAll("(?i)\\.svg$", "");
    }

    private static String hexToRgb(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return "rgb(" + r + "," + g + "," + b + ")";
    }

    /** 简易命令行入口（可手动跑）。 */
    public static void main(String[] args) {
        PackConfig cfg = PackConfig.builder()
                .inputDir(args.length > 0 ? args[0] : "E:/idea/workspace2/test/icons")
                .outputDir(args.length > 1 ? args[1] : "assets/bs/icons")
                .iconSize(args.length > 2 ? Integer.parseInt(args[2]) : 32)
                .atlasSize(args.length > 3 ? Integer.parseInt(args[3]) : 1024)
                .fillColor("#FFFFFF")
                .build();
        pack(cfg, p -> System.out.printf("[%d/%d] %s%n", p.done, p.total, p.current));
    }
}
