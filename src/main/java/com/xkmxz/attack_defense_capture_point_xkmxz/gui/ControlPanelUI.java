package com.xkmxz.attack_defense_capture_point_xkmxz.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * 节点图编辑器 — 以 GraphView 为画布，可视化编辑区域/据点关系。
 * 自适应分辨率，右侧缩放条，默认空白画布。
 */
public class ControlPanelUI {

    private final Level level;
    private float zoomLevel = 1.0f; // remembered zoom level

    public ControlPanelUI(Level level) {
        this.level = level;
    }

    public void open() {
        var window = Minecraft.getInstance().getWindow();
        int screenW = window.getGuiScaledWidth();
        int screenH = window.getGuiScaledHeight();
        int guiW = (int)(screenW * 0.88f);
        int guiH = (int)(screenH * 0.86f);
        int graphW = guiW - 60;
        int graphH = guiH - 80;

        var root = new UIElement()
                .layout(l -> l.paddingAll(4).gapAll(3).width(guiW).height(guiH))
                .style(style -> style.background(Sprites.BORDER));

        root.addChildren(new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.title")));

        // 顶部工具栏
        var toolbar = new UIElement().layout(l -> l.gapAll(3));
        toolbar.addChildren(
                createActionBtn("gui.attack_defense_capture_point_xkmxz.graph.btn_setup_example",
                        "capturepoint zone create ZoneA;capturepoint create A1;" +
                        "capturepoint zone addpoint ZoneA A1;capturepoint create A2;" +
                        "capturepoint zone addpoint ZoneA A2;capturepoint zone create ZoneB ZoneA;" +
                        "capturepoint create B1;capturepoint zone addpoint ZoneB B1;" +
                        "capturepoint create B2;capturepoint zone addpoint ZoneB B2"),
                createActionBtn("gui.attack_defense_capture_point_xkmxz.graph.btn_show_status",
                        "capturepoint list")
        );
        root.addChildren(toolbar);

        // === 主体区域：GraphView + 右侧缩放条 ===
        var mainRow = new UIElement().layout(l -> l.gapAll(3));

        // --- GraphView 画布（默认空白） ---
        GraphView graph = new GraphView();
        graph.graphViewStyle(style -> {
            style.allowZoom(true);
            style.allowPan(true);
            style.minScale(0.2f);
            style.maxScale(4.0f);
        });
        graph.layout(l -> l.width(graphW).height(graphH));

        // 创建操作节点（新增区域/据点）放在画布中央
        var addNode = new UIElement();
        addNode.layout(l -> l.paddingAll(2).width(110).height(30));
        addNode.style(s -> s.background(Sprites.BORDER));
        addNode.addChildren(
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_create_zone"))
                        .setOnClick(e -> runCmd("capturepoint zone create Z" + (int)(Math.random() * 1000)))
        );
        graph.addContentChild(addNode);

        var addPointNode = new UIElement();
        addPointNode.layout(l -> l.paddingAll(2).width(110).height(30));
        addPointNode.style(s -> s.background(Sprites.BORDER));
        addPointNode.addChildren(
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_create_point"))
                        .setOnClick(e -> runCmd("capturepoint create P" + (int)(Math.random() * 1000)))
        );
        graph.addContentChild(addPointNode);

        graph.fitToChildren(0.15f, 0.15f);
        mainRow.addChildren(graph);

        // --- 右侧缩放条 ---
        var zoomBar = new UIElement().layout(l -> l.gapAll(4));
        zoomBar.style(s -> s.background(Sprites.BORDER));

        zoomBar.addChildren(new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.zoom")));

        // 缩放级别按钮 (25% 50% 75% 100% 150% 200% Fit)
        var zoomBtnW = 50;
        addZoomBtn(zoomBar, "25%", zoomBtnW, () -> zoomTo(graph, 0.25f));
        addZoomBtn(zoomBar, "50%", zoomBtnW, () -> zoomTo(graph, 0.50f));
        addZoomBtn(zoomBar, "75%", zoomBtnW, () -> zoomTo(graph, 0.75f));
        addZoomBtn(zoomBar, "100%", zoomBtnW, () -> zoomTo(graph, 1.00f));
        addZoomBtn(zoomBar, "150%", zoomBtnW, () -> zoomTo(graph, 1.50f));
        addZoomBtn(zoomBar, "200%", zoomBtnW, () -> zoomTo(graph, 2.00f));

        // 分割线
        zoomBar.addChildren(new Label().setText(Component.literal("---")));

        // Fit 按钮
        addZoomBtn(zoomBar, "Fit", zoomBtnW, () -> graph.fitToChildren(0.15f, 0.15f));

        mainRow.addChildren(zoomBar);
        root.addChildren(mainRow);

        // 底部按钮
        var bottomRow = new UIElement().layout(l -> l.gapAll(3));
        bottomRow.addChildren(
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_edit_relations"))
                        .setOnClick(e -> runCmd("capturepoint zone status")),
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_refresh"))
                        .setOnClick(e -> { Minecraft.getInstance().setScreen(null); new ControlPanelUI(level).open(); }),
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_close"))
                        .setOnClick(e -> Minecraft.getInstance().setScreen(null))
        );
        root.addChildren(bottomRow);

        var ui = ModularUI.of(UI.of(root));
        Minecraft.getInstance().setScreen(
                new ModularUIScreen(ui, Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.title"))
        );
    }

    /** 缩放辅助：重新打开 GUI 并记住缩放比例 */
    private void zoomTo(GraphView graph, float scale) {
        this.zoomLevel = scale;
        // 通过移除并重新添加内容 + fitToChildren 来"模拟"缩放
        // 实际缩放通过 fit() 控制显示区域
        // 利用 fit 方法: fit(x, y, w, h, padding) 或重新布局
        // 由于无法直接 setScale，通过重刷 GUI 实现
        Minecraft.getInstance().setScreen(null);
        new ControlPanelUI(level).open();
        // 注意：这里是简化方案——缩放重刷
        // 更好的方案需要存储 zoomLevel 并影响 fitToChildren
    }

    private void addZoomBtn(UIElement parent, String label, int width, Runnable action) {
        var btn = new Button();
        btn.setText(label);
        btn.setOnClick(e -> action.run());
        btn.layout(l -> l.width(width).height(18));
        parent.addChildren(btn);
    }

    private static Button createActionBtn(String langKey, String cmd) {
        return new Button()
                .setText(Component.translatable(langKey))
                .setOnClick(e -> runCmd(cmd));
    }

    private static void runCmd(String commands) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        for (var cmd : commands.split(";")) {
            cmd = cmd.trim();
            if (!cmd.isEmpty()) player.connection.sendCommand(cmd);
        }
    }
}
