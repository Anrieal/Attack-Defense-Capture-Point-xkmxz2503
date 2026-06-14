package com.xkmxz.attack_defense_capture_point_xkmxz.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.xkmxz.attack_defense_capture_point_xkmxz.manager.CaptureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * 攻防战据点图编辑器 — 基于 graphif 架构的 Canvas2D 节点图编辑器。
 * <p>
 * 使用纯画布渲染（类似 graphif 的 Canvas2D + Camera 系统），
 * 节点和连线直接在 GraphView 上绘制，支持拖拽、选中和高亮。
 */
public class ControlPanelUI {

    // ---- 颜色常量 (ARGB) ----
    private static final int COLOR_BG_DARK = 0xCC1A1A2E;
    private static final int COLOR_BG_POINT_CAPTURED = 0xCC1B5E20;
    private static final int COLOR_BORDER_POINT_CAPTURED = 0xFF66BB6A;
    private static final int COLOR_BG_POINT_FREE = 0xCC263238;
    private static final int COLOR_BORDER_POINT_FREE = 0xFF78909C;
    private static final int COLOR_BG_ZONE_CAPTURED = 0xCC0D47A1;
    private static final int COLOR_BORDER_ZONE_CAPTURED = 0xFF42A5F5;
    private static final int COLOR_BG_ZONE_LOCKED = 0xCCBF360C;
    private static final int COLOR_BORDER_ZONE_LOCKED = 0xFFFF7043;
    private static final int COLOR_BG_ZONE_FREE = 0xCC4A148C;
    private static final int COLOR_BORDER_ZONE_FREE = 0xFFAB47BC;
    private static final int COLOR_EDGE_MEMBERSHIP = 0xAA90CAF9;
    private static final int COLOR_EDGE_DEPENDENCY = 0xAAFFAB91;
    private static final int COLOR_SELECTION_HIGHLIGHT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xAAFFFFFF;
    private static final int COLOR_TOOLTIP_BG = 0xCC000000;

    private final Level level;

    // 图数据模型
    private final List<NodeData> nodes = new ArrayList<>();
    private final List<EdgeData> edges = new ArrayList<>();

    // 交互状态
    private NodeData selectedNode;
    private NodeData draggingNode;
    private float dragStartMX, dragStartMY;
    private float dragOrigNX, dragOrigNY;

    // 画布引用
    private GraphView graphView;
    private GraphCanvas canvas;

    public ControlPanelUI(Level level) {
        this.level = level;
    }

    public void open() {
        var mc = Minecraft.getInstance();
        var window = mc.getWindow();
        int screenW = window.getGuiScaledWidth();
        int screenH = window.getGuiScaledHeight();
        int guiW = (int) (screenW * 0.88f);
        int guiH = (int) (screenH * 0.86f);
        int sidebarW = 60;
        int graphW = guiW - sidebarW - 14;
        int graphH = guiH - 80;

        var root = new UIElement()
                .layout(l -> l.paddingAll(4).gapAll(3).width(guiW).height(guiH))
                .style(s -> s.background(Sprites.BORDER));

        // === 左侧主区域 ===
        var mainArea = new UIElement()
                .layout(l -> l.width(graphW).heightPercent(100).gapAll(3));

        mainArea.addChildren(new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.title")));

        // 顶部工具栏
        var toolbar = new UIElement().layout(l -> l.gapAll(3));
        toolbar.addChildren(
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_create_point"))
                        .setOnClick(e -> runCmd("capturepoint create P" + (int) (Math.random() * 1000) + ";")),
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_create_zone"))
                        .setOnClick(e -> runCmd("capturepoint zone create Z" + (int) (Math.random() * 1000) + ";")),
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_show_status"))
                        .setOnClick(e -> runCmd("capturepoint list")),
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.btn_refresh"))
                        .setOnClick(e -> {
                            mc.setScreen(null);
                            new ControlPanelUI(level).open();
                        }),
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.block.close"))
                        .setOnClick(e -> mc.setScreen(null))
        );
        mainArea.addChildren(toolbar);

        // === GraphView 画布 (Camera System) ===
        graphView = new GraphView();
        graphView.graphViewStyle(style -> {
            style.allowZoom(true);
            style.allowPan(true);
            style.minScale(0.2f);
            style.maxScale(4.0f);
        });
        graphView.layout(l -> l.widthPercent(100).height(graphH));

        // 自定义画布层
        canvas = new GraphCanvas();
        graphView.addContentChild(canvas);

        // 加载数据
        loadData();

        if (nodes.isEmpty()) {
            var hintNode = new UIElement();
            hintNode.layout(l -> l.paddingAll(8).width(200).height(50));
            hintNode.addChildren(new Label()
                    .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.empty_hint")));
            graphView.addContentChild(hintNode);
        }

        if (!nodes.isEmpty()) {
            graphView.fitToChildren(60f, 0.15f);
        }

        mainArea.addChildren(graphView);

        // === 右侧缩放边栏 ===
        var sidebar = new UIElement()
                .layout(l -> l.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN)
                        .width(sidebarW)
                        .heightPercent(100)
                        .paddingAll(4)
                        .gapAll(4))
                .style(s -> s.background(Sprites.BORDER));

        sidebar.addChildren(new Label()
                .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.zoom"))
                .layout(l -> l.widthPercent(100)));

        // 缩放滑块（自定义，通过鼠标拖拽调节）
        var slider = new ZoomSlider(graphView);
        slider.layout(l -> l.width(12).height(120));
        // 居中
        var sliderWrapper = new UIElement()
                .layout(l -> l.justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER)
                        .widthPercent(100));
        sliderWrapper.addChildren(slider);
        sidebar.addChildren(sliderWrapper);

        // 当前缩放百分比
        var zoomLabel = new Label();
        zoomLabel.setText(getZoomText(graphView.getScale()));
        zoomLabel.layout(l -> l.widthPercent(100));
        sidebar.addChildren(zoomLabel);

        // Fit 按钮
        var fitBtn = new Button()
                .setText("Fit")
                .setOnClick(e -> graphView.fitToChildren(60f, 0.15f));
        fitBtn.layout(l -> l.widthPercent(100).height(18));
        sidebar.addChildren(fitBtn);

        // 监听缩放变化更新显示
        graphView.addEventListener(UIEvents.MOUSE_WHEEL, ev -> {
            zoomLabel.setText(getZoomText(graphView.getScale()));
        });

        root.addChildren(mainArea, sidebar);

        var ui = ModularUI.of(UI.of(root));
        mc.setScreen(
                new ModularUIScreen(ui, Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.title"))
        );
    }

    private static String getZoomText(float scale) {
        return (int) (scale * 100) + "%";
    }

    // ============================================================
    //  数据模型
    // ============================================================

    enum NodeKind { POINT, ZONE }

    static class NodeData {
        String name;
        NodeKind kind;
        boolean captured;
        String ownerOrLocked; // for points: owner name; for zones: requiredZone if locked
        float x, y;           // position in world space (center of node)
        float w, h;           // size (width, height)
        boolean selected;

        NodeData(String name, NodeKind kind, boolean captured, String ownerOrLocked) {
            this.name = name;
            this.kind = kind;
            this.captured = captured;
            this.ownerOrLocked = ownerOrLocked;
            this.w = 160;
            this.h = 48;
        }
    }

    enum EdgeKind { MEMBERSHIP, DEPENDENCY }

    record EdgeData(NodeData from, NodeData to, EdgeKind kind) {}

    // ============================================================
    //  数据加载
    // ============================================================

    private void loadData() {
        var serverLevel = getServerLevel();
        if (serverLevel == null) return;

        var manager = CaptureManager.get(serverLevel);
        var points = manager.getPoints();
        var zones = manager.getZones();
        if (points.isEmpty() && zones.isEmpty()) return;

        // 布局计算：网格布局
        float startX = 80f;
        float startY = 60f;
        float gapX = 200f;
        float gapY = 100f;

        float curX = startX;
        float curY = startY + 60f;
        int idx = 0;

        for (var entry : points.values()) {
            boolean captured = entry.owner() != null;
            var node = new NodeData(entry.name(), NodeKind.POINT, captured,
                    captured ? entry.owner() : null);
            node.x = curX + 80;
            node.y = curY + 24;
            nodes.add(node);
            idx++;
            curX += gapX;
            if (idx % 4 == 0) { curX = startX; curY += gapY; }
        }

        curX = startX + 80f;
        curY += 60f;
        idx = 0;

        for (var entry : zones.values()) {
            boolean captured = manager.isZoneCaptured(entry.name());
            boolean accessible = manager.canAccessZone(entry.name());
            var node = new NodeData(entry.name(), NodeKind.ZONE, captured,
                    accessible ? null : entry.requiredZone());
            node.x = curX + 80;
            node.y = curY + 24;
            nodes.add(node);
            idx++;
            curX += gapX;
            if (idx % 4 == 0) { curX = startX + 80f; curY += gapY; }
        }

        // 注册边
        for (var entry : points.values()) {
            var pn = findNode(entry.name());
            if (pn == null) continue;
            for (var ze : zones.values()) {
                if (ze.capturePoints().contains(entry.name())) {
                    var zn = findNode(ze.name());
                    if (zn != null) edges.add(new EdgeData(pn, zn, EdgeKind.MEMBERSHIP));
                }
            }
        }
        for (var entry : zones.values()) {
            if (entry.requiredZone() != null) {
                var zn = findNode(entry.name());
                var dn = findNode(entry.requiredZone());
                if (zn != null && dn != null) edges.add(new EdgeData(dn, zn, EdgeKind.DEPENDENCY));
            }
        }
    }

    private NodeData findNode(String name) {
        for (var n : nodes) if (n.name.equals(name)) return n;
        return null;
    }

    // ============================================================
    //  画布渲染 (Canvas2D — 类似 graphif 的 Canvas + Renderer)
    // ============================================================

    private class GraphCanvas extends UIElement {

        GraphCanvas() {
            // 鼠标事件处理：选中、拖拽
            addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragUpdate);
            addEventListener(UIEvents.DRAG_END, this::onDragEnd);
        }

        @Override
        public void drawBackgroundAdditional(GUIContext ctx) {
            if (nodes.isEmpty()) return;

            // 1. 绘制所有边（在节点下方）
            for (var edge : edges) {
                drawEdge(ctx, edge);
            }

            // 2. 绘制所有节点
            for (var node : nodes) {
                drawNode(ctx, node);
            }

            // 3. 绘制选中节点的详情提示
            if (selectedNode != null) {
                drawSelectionInfo(ctx, selectedNode);
            }
        }

        // ---- 节点绘制 ----

        private void drawNode(GUIContext ctx, NodeData node) {
            float rx = node.x - node.w / 2f;
            float ry = node.y - node.h / 2f;

            int bgColor, borderColor;
            if (node.kind == NodeKind.POINT) {
                if (node.captured) {
                    bgColor = COLOR_BG_POINT_CAPTURED;
                    borderColor = node.selected ? COLOR_SELECTION_HIGHLIGHT : COLOR_BORDER_POINT_CAPTURED;
                } else {
                    bgColor = COLOR_BG_POINT_FREE;
                    borderColor = node.selected ? COLOR_SELECTION_HIGHLIGHT : COLOR_BORDER_POINT_FREE;
                }
            } else {
                if (node.captured) {
                    bgColor = COLOR_BG_ZONE_CAPTURED;
                    borderColor = node.selected ? COLOR_SELECTION_HIGHLIGHT : COLOR_BORDER_ZONE_CAPTURED;
                } else if (node.ownerOrLocked != null) {
                    bgColor = COLOR_BG_ZONE_LOCKED;
                    borderColor = node.selected ? COLOR_SELECTION_HIGHLIGHT : COLOR_BORDER_ZONE_LOCKED;
                } else {
                    bgColor = COLOR_BG_ZONE_FREE;
                    borderColor = node.selected ? COLOR_SELECTION_HIGHLIGHT : COLOR_BORDER_ZONE_FREE;
                }
            }

            // 圆角矩形本体
            DrawerHelper.drawSolidRect(ctx.graphics, rx, ry, node.w, node.h, bgColor);

            // 选中高亮边框（外扩 2px）
            if (node.selected) {
                DrawerHelper.drawBorder(ctx.graphics, rx - 2, ry - 2, node.w + 4, node.h + 4, borderColor, 2);
            }

            // 普通边框
            DrawerHelper.drawBorder(ctx.graphics, rx, ry, node.w, node.h, borderColor, 1);

            // 文本：图标 + 名称
            String icon = switch (node.kind) {
                case POINT -> node.captured ? "\u2691" : "\u25CB";
                case ZONE -> node.captured ? "\u25A0" : "\u25A1";
            };
            String label = icon + " " + node.name;

            Font font = Minecraft.getInstance().font;
            // 文字居中
            float textX = rx + 8;
            float textY = ry + (node.h - font.lineHeight) / 2f + 1;
            DrawerHelper.drawText(ctx.graphics, label, textX, textY, 1.0f, COLOR_TEXT_WHITE);

            // 状态标签（右下角小字）
            String status = switch (node.kind) {
                case POINT -> node.captured ? node.ownerOrLocked : "\u25CB";
                case ZONE -> {
                    if (node.captured) yield "Captured";
                    else if (node.ownerOrLocked != null) yield "Locked";
                    else yield "Free";
                }
            };
            if (node.kind == NodeKind.ZONE || (node.kind == NodeKind.POINT && node.captured)) {
                float sw = font.width(status);
                DrawerHelper.drawText(ctx.graphics, status,
                        rx + node.w - sw - 6, ry + node.h - font.lineHeight - 2,
                        0.7f, COLOR_TEXT_DIM);
            }
        }

        // ---- 边绘制 ----

        private void drawEdge(GUIContext ctx, EdgeData edge) {
            float x1 = edge.from.x;
            float y1 = edge.from.y + edge.from.h / 2f;
            float x2 = edge.to.x;
            float y2 = edge.to.y - edge.to.h / 2f;

            int color = switch (edge.kind) {
                case MEMBERSHIP -> COLOR_EDGE_MEMBERSHIP;
                case DEPENDENCY -> COLOR_EDGE_DEPENDENCY;
            };
            float lineWidth = switch (edge.kind) {
                case MEMBERSHIP -> 1.5f;
                case DEPENDENCY -> 2.0f;
            };

            // 贝塞尔曲线：从 from 底部到 to 顶部
            drawBezier(ctx, x1, y1, x2, y2, color, lineWidth);
        }

        private void drawBezier(GUIContext ctx, float x1, float y1, float x2, float y2,
                                 int color, float width) {
            float midY = (y1 + y2) / 2f;
            var points = new ArrayList<Vector2f>();
            int segments = 24;
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                float t1 = 1 - t;
                float px = t1 * t1 * t1 * x1 + 3 * t1 * t1 * t * x1 + 3 * t1 * t * t * x2 + t * t * t * x2;
                float py = t1 * t1 * t1 * y1 + 3 * t1 * t1 * t * midY + 3 * t1 * t * t * midY + t * t * t * y2;
                points.add(new Vector2f(px, py));
            }
            DrawerHelper.drawLines(ctx.graphics, points, color, color, width);

            // 终点箭头
            drawArrowhead(ctx, x1, y1, x2, y2, color);
        }

        private void drawArrowhead(GUIContext ctx, float x1, float y1, float x2, float y2, int color) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1f) return;
            float ux = dx / len;
            float uy = dy / len;
            float arrowSize = 8f;
            float px = x2 - ux * 6f;
            float py = y2 - uy * 6f;
            float ax = -uy * arrowSize * 0.4f;
            float ay = ux * arrowSize * 0.4f;

            var arrow = new ArrayList<Vector2f>();
            arrow.add(new Vector2f(x2, y2));
            arrow.add(new Vector2f(px + ax, py + ay));
            arrow.add(new Vector2f(px - ax, py - ay));
            DrawerHelper.drawLines(ctx.graphics, arrow, color, color, 1.0f);
        }

        // ---- 选中信息 ----

        private void drawSelectionInfo(GUIContext ctx, NodeData node) {
            String info = switch (node.kind) {
                case POINT -> {
                    if (node.captured) yield "Owner: " + node.ownerOrLocked;
                    else yield "Free point";
                }
                case ZONE -> {
                    if (node.captured) yield "Zone captured";
                    else if (node.ownerOrLocked != null) yield "Requires: " + node.ownerOrLocked;
                    else yield "Free zone";
                }
            };
            float rx = node.x - node.w / 2f;
            float ty = node.y + node.h / 2f + 4;
            DrawerHelper.drawText(ctx.graphics, info, rx, ty, 0.8f, COLOR_TEXT_DIM);
        }

        // ============================================================
        //  鼠标交互
        // ============================================================

        private void onMouseDown(UIEvent event) {
            if (event.button != 0) return;
            float scale = getParentScale();
            float mx = (event.x - getPositionX()) / scale + graphView.getOffsetX();
            float my = (event.y - getPositionY()) / scale + graphView.getOffsetY();

            // 选中检测（反向遍历，上层节点优先）
            NodeData hit = null;
            for (int i = nodes.size() - 1; i >= 0; i--) {
                var n = nodes.get(i);
                if (mx >= n.x - n.w / 2f && mx <= n.x + n.w / 2f &&
                    my >= n.y - n.h / 2f && my <= n.y + n.h / 2f) {
                    hit = n;
                    break;
                }
            }

            // 更新选中状态
            for (var n : nodes) n.selected = false;
            if (hit != null) {
                hit.selected = true;
                selectedNode = hit;
                // 开始拖拽
                draggingNode = hit;
                // 计算拖拽起始位置（世界坐标）
                dragOrigNX = hit.x;
                dragOrigNY = hit.y;
                // 记录鼠标起始位置（局部坐标）
                dragStartMX = event.x;
                dragStartMY = event.y;
            } else {
                selectedNode = null;
                draggingNode = null;
            }
        }

        private void onDragUpdate(UIEvent event) {
            if (draggingNode == null) return;

            float scale = getParentScale();
            float dx = (event.x - dragStartMX) / scale;
            float dy = (event.y - dragStartMY) / scale;

            draggingNode.x = dragOrigNX + dx;
            draggingNode.y = dragOrigNY + dy;
        }

        private void onDragEnd(UIEvent event) {
            draggingNode = null;
        }

        private float getParentScale() {
            var p = getParent();
            while (p != null) {
                if (p instanceof GraphView gv) return gv.getScale();
                p = p.getParent();
            }
            return 1f;
        }
    }

    // ============================================================
    //  工具方法
    // ============================================================

    @org.jetbrains.annotations.Nullable
    private net.minecraft.server.level.ServerLevel getServerLevel() {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) return sl;
        var mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().getLevel(level.dimension());
        }
        return null;
    }

    /**
     * 垂直滑块 — 通过鼠标拖拽调节 GraphView 缩放比例。
     */
    private static class ZoomSlider extends UIElement {
        private final GraphView target;
        private float value = 0.5f; // 0~1, 映射到 minScale~maxScale
        private static final int BAR_COLOR = 0xCC666666;
        private static final int THUMB_COLOR = 0xFFAAAAAA;
        private static final int TRACK_COLOR = 0x44444444;

        ZoomSlider(GraphView target) {
            this.target = target;
            style(s -> s.background(Sprites.RECT_SOLID)
                    .backgroundTexture(new ColorRectTexture(TRACK_COLOR)));
            addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragUpdate);
        }

        @Override
        public void drawBackgroundAdditional(GUIContext ctx) {
            float h = getSizeHeight();
            float w = getSizeWidth();
            float thumbH = 16f;

            // 滑块轨道
            DrawerHelper.drawSolidRect(ctx.graphics, 0, 0, w, h, TRACK_COLOR);

            // 已填充部分
            float fillH = h * value;
            DrawerHelper.drawSolidRect(ctx.graphics, 0, h - fillH, w, fillH, BAR_COLOR);

            // 滑块thumb
            float thumbY = h - fillH - thumbH / 2f;
            DrawerHelper.drawSolidRect(ctx.graphics, 0, thumbY, w, thumbH, THUMB_COLOR);
        }

        private void onMouseDown(UIEvent event) {
            if (event.button != 0) return;
            updateValue(event.y);
            startDrag(this, null);
        }

        private void onDragUpdate(UIEvent event) {
            updateValue(event.y);
        }

        private void updateValue(float mouseY) {
            float h = getSizeHeight();
            if (h <= 0) return;
            // 从底部计算比率
            value = Mth.clamp(1f - (mouseY - getPositionY()) / h, 0f, 1f);
            applyZoom();
        }

        private void applyZoom() {
            float minS = target.getGraphViewStyle().minScale();
            float maxS = target.getGraphViewStyle().maxScale();
            float newScale = Mth.clamp(minS + (maxS - minS) * value, minS, maxS);
            // 通过 reflect 设置 scale（GraphView 只有 @Getter 没有 @Setter）
            try {
                var field = GraphView.class.getDeclaredField("scale");
                field.setAccessible(true);
                field.setFloat(target, newScale);
                var method = GraphView.class.getDeclaredMethod("refreshContentTransform");
                method.setAccessible(true);
                method.invoke(target);
            } catch (Exception ignored) {
            }
        }
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
