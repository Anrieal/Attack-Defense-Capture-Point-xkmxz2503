package com.xkmxz.attack_defense_capture_point_xkmxz.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.network.chat.Component;

/**
 * 统一主题配色和 UI 工具类 — 参考 synaxis 的 CIRCUIT Theme 实现。<br>
 * 使用 SDFRectTexture 绘制圆角矩形面板和按钮，提供现代化的深色主题外观。
 */
public final class CapturePointTheme {

    // ================================================================
    //  颜色常量 — 参考 synaxis CIRCUIT Theme
    // ================================================================

    /** 主背景色 — 深蓝黑 */
    public static final int ROOT_COLOR = 0xFF0F0F1A;
    /** 面板背景色 — 深紫黑 */
    public static final int PANEL_COLOR = 0xFF1A1A2E;
    /** 输入框/字段背景色 */
    public static final int FIELD_COLOR = 0xFF16213E;
    /** 边框颜色 */
    public static final int BORDER_COLOR = 0xFF2A2A4A;
    /** 按钮默认背景 */
    public static final int BUTTON_COLOR = 0xFF1E2A4A;
    /** 按钮悬浮背景 */
    public static final int BUTTON_HOVER_COLOR = 0xFF2A3A5A;
    /** 按钮按下背景 */
    public static final int BUTTON_PRESSED_COLOR = 0xFF3A4A6A;
    /** 按钮选中/激活背景 */
    public static final int BUTTON_SELECTED_COLOR = 0xFFAA3333;
    /** 文字主色 */
    public static final int TEXT_COLOR = 0xFFE0E0E0;
    /** 文字次要色 */
    public static final int TEXT_SECONDARY = 0xFF888888;
    /** 文字强调色 */
    public static final int TEXT_ACCENT = 0xFFFFAA33;
    /** 分割线颜色 */
    public static final int SEPARATOR_COLOR = 0xFF2A2A4A;

    // 判断器节点颜色
    public static final int DECISION_COLOR = 0xFFFF9800;

    // ================================================================
    //  尺寸常量
    // ================================================================

    /** 圆角半径 */
    public static final float CORNER_RADIUS = 4.0f;
    /** 边框粗细 */
    public static final float BORDER_STROKE = 1.0f;
    /** 按钮中边框粗细 */
    public static final float BUTTON_STROKE = 0.5f;

    // ================================================================
    //  SDFRectTexture 工厂方法
    // ================================================================

    /** 创建面板背景纹理（填充色 + 边框） */
    public static IGuiTexture panelBg(int fillColor) {
        return SDFRectTexture.of(fillColor)
                .setBorderColor(BORDER_COLOR)
                .setRadius(CORNER_RADIUS)
                .setStroke(BORDER_STROKE);
    }

    /** 创建按钮纹理（填充色 + 边框，无悬浮效果） */
    public static IGuiTexture buttonBg(int fillColor) {
        return SDFRectTexture.of(fillColor)
                .setBorderColor(BORDER_COLOR)
                .setRadius(CORNER_RADIUS)
                .setStroke(BUTTON_STROKE);
    }

    /** 创建按钮纹理（带悬浮高亮） */
    public static IGuiTexture buttonBg(int fillColor, boolean hovered, boolean pressed) {
        int color = pressed ? BUTTON_PRESSED_COLOR : (hovered ? BUTTON_HOVER_COLOR : fillColor);
        return SDFRectTexture.of(color)
                .setBorderColor(hovered ? TEXT_SECONDARY : BORDER_COLOR)
                .setRadius(CORNER_RADIUS)
                .setStroke(BUTTON_STROKE);
    }

    /** 纯色背景（无边框） */
    public static IGuiTexture solidBg(int color) {
        return new ColorRectTexture(color);
    }

    // ================================================================
    //  UIElement 工厂方法
    // ================================================================

    /** 创建一个带 SDF 背景的面板容器 */
    public static UIElement panel() {
        return new UIElement()
                .style(s -> s.background(Sprites.BORDER)
                        .backgroundTexture(panelBg(PANEL_COLOR)));
    }

    /** 创建一个带 SDF 背景的面板容器（指定填充色） */
    public static UIElement panel(int fillColor) {
        return new UIElement()
                .style(s -> s.background(Sprites.BORDER)
                        .backgroundTexture(panelBg(fillColor)));
    }

    /** 创建一个主题风格的按钮 */
    public static Button styledButton(Component text) {
        var btn = new Button();
        btn.setText(text);
        btn.style(s -> s.background(Sprites.BORDER)
                .backgroundTexture(buttonBg(BUTTON_COLOR)));
        // 添加悬浮/按下事件
        btn.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_ENTER, ev -> {
            btn.style(s -> s.backgroundTexture(buttonBg(BUTTON_COLOR, true, false)));
        });
        btn.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_LEAVE, ev -> {
            btn.style(s -> s.backgroundTexture(buttonBg(BUTTON_COLOR, false, false)));
        });
        btn.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_DOWN, ev -> {
            btn.style(s -> s.backgroundTexture(buttonBg(BUTTON_COLOR, true, true)));
        });
        btn.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.MOUSE_UP, ev -> {
            btn.style(s -> s.backgroundTexture(buttonBg(BUTTON_COLOR, true, false)));
        });
        return btn;
    }

    /** 创建一个主题风格的标签 */
    public static Label styledLabel(Component text, int color, float fontSize) {
        var label = new Label();
        label.setText(text);
        label.textStyle(s -> {
            s.fontSize(fontSize);
            s.textColor(color);
            s.textShadow(true);
        });
        return label;
    }

    /** 创建一个标题标签 */
    public static Label titleLabel(Component text) {
        return styledLabel(text, TEXT_COLOR, 11.0f);
    }

    /** 创建一个次要标签 */
    public static Label secondaryLabel(Component text) {
        return styledLabel(text, TEXT_SECONDARY, 9.0f);
    }

    /** 创建分割线 */
    public static UIElement separator() {
        return new UIElement()
                .layout(l -> l.widthPercent(100).height(1))
                .style(s -> s.backgroundTexture(new ColorRectTexture(SEPARATOR_COLOR)));
    }

    private CapturePointTheme() {}
}
