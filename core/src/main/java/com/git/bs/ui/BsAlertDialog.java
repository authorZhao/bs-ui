package com.git.bs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Bootstrap 风格 Alert 弹窗（基于 {@link BsModal}）。
 *
 * <p>4 种级别：</p>
 * <ul>
 *   <li>{@link Level#NOTICE} —— 通知（蓝色，primary）</li>
 *   <li>{@link Level#WARNING} —— 警告（黄色，warning）</li>
 *   <li>{@link Level#ERROR} —— 错误（红色，danger）</li>
 *   <li>{@link Level#SUCCESS} —— 成功（绿色，success）</li>
 * </ul>
 *
 * <p>每种级别有自己的图标色块（标题前）+ 入场动画（NOTICE 淡入、WARNING 下滑入、
 * ERROR 缩放进入、SUCCESS 淡入）+ 配色按钮。</p>
 *
 * <p>用法：</p>
 * <pre>{@code
 * BsAlertDialog.show(stage, skin, BsAlertDialog.Level.WARNING, "标题", "消息内容");
 *
 * // 或者 builder 风格
 * new BsAlertDialog("标题", "消息", Level.ERROR, skin)
 *     .setOnClose(() -> setStatus("弹窗已关"))
 *     .showModal(stage);
 * }</pre>
 */
@Slf4j
public class BsAlertDialog extends BsModal {

    public enum Level { NOTICE, WARNING, ERROR, SUCCESS }

    @Getter
    private final Level level;

    private Runnable onClose;

    public BsAlertDialog(String title, String message, Level level, Skin skin) {
        super(title == null ? levelText(level) : title, skin);
        this.level = level;
        configByLevel(level);

        // 内容：消息文本
        Label msg = new Label(message == null ? "" : message, skin);
        msg.setColor(BsTheme.ts());
        msg.setWrap(true);
        content(msg).contentWidth(380);

        // 底部"知道了"按钮（颜色按级别）
        addButton("知道了", () -> {
            if (onClose != null) {
                try { onClose.run(); } catch (Throwable t) { log.warn("onClose error", t); }
            }
        }, levelButtonVariant(level), BsButton.Style.SOLID);

        // 默认点 backdrop 也能关
        closeOnBackdrop(true);
    }

    /** 根据级别配置：图标色块 + 标题 banner + 入场/出场动画 + 按钮回调 */
    private void configByLevel(Level l) {
        Color accent = levelColor(BsUI.getSkin(), l);
        // 标题前小色块图标（Pixmap 已有的 white drawable 染色）
        setTitleIcon(BsUI.getSkin().newDrawable("white", accent));
        // 入场 + 出场动画：NOTICE/SUCCESS 淡入+淡出（柔和），WARNING 上滑入+下滑出，
        // ERROR 缩放进入+缩放退出。出场时长拉长让动画更明显。
        switch (l) {
            case NOTICE:
            case SUCCESS:
                setEnterAnimation(modal -> BsAnimations.fadeIn(modal, 0.3f));
                setExitAnimation((modal, done) -> BsAnimations.fadeOut(modal, 0.35f, done));
                break;
            case WARNING:
                setEnterAnimation(modal -> BsAnimations.slideInDown(modal, 0.3f));
                setExitAnimation((modal, done) -> BsAnimations.slideOutUp(modal, 0.28f, done));
                break;
            case ERROR:
                setEnterAnimation(modal -> BsAnimations.scaleIn(modal, 0.28f));
                setExitAnimation((modal, done) -> BsAnimations.scaleOut(modal, 0.25f, done));
                break;
        }
    }

    /** 自动关闭延迟（秒）；<=0 表示不自动关闭。showModal 之后生效。 */
    private float autoCloseSec = -1f;

    /** 设置自动关闭延迟（秒）。NOTICE/SUCCESS 推荐用 2~3 秒。 */
    public BsAlertDialog setAutoCloseAfter(float seconds) {
        this.autoCloseSec = seconds;
        return this;
    }

    @Override
    public void showModal(Stage stage) {
        super.showModal(stage);
        // showModal 后才调 autoCloseAfter（actor 已在 stage 上）
        if (autoCloseSec > 0) {
            autoCloseAfter(autoCloseSec);
        }
    }

    public BsAlertDialog setOnClose(Runnable r) { this.onClose = r; return this; }

    /** 便捷静态入口：构造并立即显示。 */
    public static BsAlertDialog show(Stage stage, Skin skin, Level level, String title, String message) {
        BsAlertDialog d = new BsAlertDialog(title, message, level, skin);
        d.showModal(stage);
        return d;
    }

    // ========================= 级别 → 视觉 =========================

    /** 级别 → 配色（参考 Bootstrap alert）。V2：必须传 skin。 */
    public static Color levelColor(Skin skin, Level l) {
        switch (l) {
            case NOTICE:  return BsPalette.PRIMARY.getMain();   // 蓝
            case WARNING: return BsPalette.WARNING.getMain();   // 黄
            case ERROR:   return BsPalette.DANGER.getMain();    // 红
            case SUCCESS: return BsPalette.SUCCESS.getMain();   // 绿
            default:      return Color.GRAY;
        }
    }

    /** 级别 → 标题文字（title 为 null 时使用）。 */
    public static String levelText(Level l) {
        switch (l) {
            case NOTICE:  return "通知";
            case WARNING: return "警告";
            case ERROR:   return "错误";
            case SUCCESS: return "成功";
            default:      return "提示";
        }
    }

    /** 级别 → 关闭按钮的 BsButton Variant。 */
    public static BsButton.Variant levelButtonVariant(Level l) {
        switch (l) {
            case NOTICE:  return BsButton.Variant.PRIMARY;
            case WARNING: return BsButton.Variant.WARNING;
            case ERROR:   return BsButton.Variant.DANGER;
            case SUCCESS: return BsButton.Variant.SUCCESS;
            default:      return BsButton.Variant.SECONDARY;
        }
    }
}
