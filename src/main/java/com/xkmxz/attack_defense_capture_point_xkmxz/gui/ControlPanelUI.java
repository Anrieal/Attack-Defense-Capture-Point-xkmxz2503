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
 * 攻防战据点图编辑器 — 仿 graphif 风格。
 * 所有事件直接在 GraphView 层处理（通过 addEventListener），
 * GraphCanvas 只负责渲染绘制，实现了右键菜单、左键选中拖拽、缩放滑块。
 */
public class ControlPanelUI {

    // 颜色常量
    private static final int C_PT_O_BG = 0xCC1B5E20, C_PT_O_BD = 0xFF66BB6A;
    private static final int C_PT_F_BG = 0xCC263238, C_PT_F_BD = 0xFF78909C;
    private static final int C_ZN_C_BG = 0xCC0D47A1, C_ZN_C_BD = 0xFF42A5F5;
    private static final int C_ZN_L_BG = 0xCCBF360C, C_ZN_L_BD = 0xFFFF7043;
    private static final int C_ZN_F_BG = 0xCC4A148C, C_ZN_F_BD = 0xFFAB47BC;
    private static final int C_EDGE_M = 0xAA90CAF9, C_EDGE_D = 0xAAFFAB91;
    private static final int C_SEL = 0xFFFFFFFF, C_TXT = 0xFFFFFFFF, C_TXT_D = 0xAAFFFFFF;
    private static final int C_PNL = 0xCC1E1E2E, C_PNL_BD = 0xFF444466;
    private static final int C_SLD_TK = 0x44333333, C_SLD_FL = 0xCC666666, C_SLD_TH = 0xFFAAAAAA;

    private final Level level;
    private final List<NodeData> nodes = new ArrayList<>();
    private final List<EdgeData> edges = new ArrayList<>();

    private NodeData selNode;
    private GraphCanvas canvas;

    /** 右键菜单位置（null 表示关闭） */
    private float ctxMX, ctxMY;
    private boolean ctxOpen;

    // 拖拽节点状态
    private NodeData dragNode;
    private float dragOffX, dragOffY; // 鼠标按下时节点位置
    private float dragMX, dragMY;     // 鼠标按下位置

    // 缩放滑块值
    private float sldVal = 0.5f;
    private GraphView graphView;
    private Label zl;

    public ControlPanelUI(Level level) { this.level = level; }

    public void open() {
        var mc = Minecraft.getInstance();
        var root = new UIElement()
                .layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(0))
                .style(s -> s.background(Sprites.RECT_SOLID).backgroundTexture(new ColorRectTexture(0xFF1A1A2E)));

        // === GraphView ===
        graphView = new GraphView();
        graphView.graphViewStyle(s -> { s.allowZoom(true); s.allowPan(true); s.minScale(0.2f); s.maxScale(4.0f); });
        graphView.layout(l -> l.widthPercent(100).heightPercent(100));

        // 渲染层（仅绘制，不处理事件）
        canvas = new GraphCanvas();
        graphView.addContentChild(canvas);
        root.addChildren(graphView);

        // === 全局事件：直接在 GraphView 层监听 ===
        // 鼠标按下：左键=选中/拖拽节点，右键=菜单
        graphView.addEventListener(UIEvents.MOUSE_DOWN, this::onGvMouseDown);
        // 拖拽更新：需要在 GraphView 层监听才能收到
        graphView.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onGvDragUpdate);
        graphView.addEventListener(UIEvents.DRAG_END, this::onGvDragEnd);
        // 滚轮缩放后同步滑块
        graphView.addEventListener(UIEvents.MOUSE_WHEEL, ev -> { syncSldFromScale(); });

        loadData();
        if (!nodes.isEmpty()) graphView.fitToChildren(60f, 0.15f);

        // === 底部工具栏 ===
        var tb = new UIElement().layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE).left(0).right(0).bottom(8).height(32)
                .justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER));
        var tbi = new UIElement().layout(l -> l.widthAuto().height(28).gapAll(2).paddingAll(2))
                .style(s -> s.background(Sprites.BORDER).backgroundTexture(new ColorRectTexture(C_PNL)));
        tbi.addChildren(
                btn("\u21BB", () -> { mc.setScreen(null); new ControlPanelUI(level).open(); }),
                btn("\u2715", () -> mc.setScreen(null)));
        tb.addChildren(tbi);
        root.addChildren(tb);

        // === 右侧缩放面板（独立于 GraphView，使用根级 ABSOLUTE） ===
        var rp = new UIElement().layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .right(8).topPercent(50).width(44).height(200).paddingAll(4).gapAll(4)
                .flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN))
                .style(s -> s.background(Sprites.BORDER).backgroundTexture(new ColorRectTexture(C_PNL)));

        zl = new Label(); zl.setText(pctTxt()); zl.layout(l -> l.widthPercent(100)); rp.addChildren(zl);

        // 滑块条（自定义绘制 + 独立事件处理）
        var sld = new Sld(graphView, () -> sldVal, v -> { sldVal = v; applySldZoom(); });
        sld.layout(l -> l.width(12).height(100));

        var sw = new UIElement().layout(l -> l.justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER).widthPercent(100));
        sw.addChildren(sld); rp.addChildren(sw);

        var fb = new Button(); fb.setText("Fit"); fb.layout(l -> l.widthPercent(100).height(18));
        fb.setOnClick(e -> { if (!nodes.isEmpty()) graphView.fitToChildren(60f, 0.15f); zl.setText(pctTxt()); syncSldFromScale(); });
        rp.addChildren(fb);
        root.addChildren(rp);

        graphView.addEventListener(UIEvents.MOUSE_WHEEL, ev -> { zl.setText(pctTxt()); syncSldFromScale(); });

        var ui = ModularUI.of(UI.of(root));
        mc.setScreen(new ModularUIScreen(ui, Component.translatable("gui.attack_defense_capture_point_xkmxz.graph.title")));
    }

    // ---- 工具 ----
    private static Button btn(String t, Runnable a) { var b = new Button(); b.setText(t); b.layout(l -> l.width(24).heightPercent(100)); b.setOnClick(e -> a.run()); return b; }
    private String pctTxt() { return (int)(graphView.getScale() * 100) + "%"; }
    private void syncSldFromScale() {
        float sc = graphView.getScale(), mn = graphView.getGraphViewStyle().minScale(), mx = graphView.getGraphViewStyle().maxScale();
        sldVal = Mth.clamp((sc - mn) / (mx - mn), 0f, 1f);
    }

    private void applySldZoom() {
        float mn = graphView.getGraphViewStyle().minScale(), mx = graphView.getGraphViewStyle().maxScale();
        float ns = Mth.clamp(mn + (mx - mn) * sldVal, mn, mx);
        try {
            var f = GraphView.class.getDeclaredField("scale"); f.setAccessible(true); f.setFloat(graphView, ns);
            var m = GraphView.class.getDeclaredMethod("refreshContentTransform"); m.setAccessible(true); m.invoke(graphView);
        } catch (Exception ignored) {}
    }

    /** 计算元素在屏幕上的绝对 Y 位置 */
    private static float screenYLocal(UIElement el, float screenY) {
        float y = 0;
        while (el != null) { y += el.getPositionY(); el = el.getParent(); }
        return screenY - y;
    }

    // ================================================================
    //  事件处理（在 GraphView 层）
    // ================================================================

    private void onGvMouseDown(UIEvent ev) {
        // 计算画布世界坐标
        float scale = graphView.getScale();
        float wx = (ev.x - graphView.getPositionX())/scale + graphView.getOffsetX();
        float wy = (ev.y - graphView.getPositionY())/scale + graphView.getOffsetY();

        // --- 右键：菜单 ---
        if (ev.button == 1) {
            ctxOpen = true;
            ctxMX = ev.x - 40;
            ctxMY = ev.y - 20;
            return;
        }
        // --- 左键：关闭菜单 / 选中节点 / 拖拽 ---
        if (ev.button == 0) {
            // 先检查是否点击了菜单项
            if (ctxOpen) {
                int idx = (int)((ev.y - ctxMY - 2) / 17f);
                if (ev.x >= ctxMX && ev.x <= ctxMX + 120 && idx >= 0 && idx <= 4) {
                    execCtx(idx);
                    ctxOpen = false;
                    return;
                }
                ctxOpen = false;
            }
            // 命中检测
            NodeData hit = null;
            for (int i = nodes.size() - 1; i >= 0; i--) {
                var n = nodes.get(i);
                if (wx >= n.x - n.w/2f && wx <= n.x + n.w/2f && wy >= n.y - n.h/2f && wy <= n.y + n.h/2f) {
                    hit = n; break;
                }
            }
            for (var n : nodes) n.selected = false;
            if (hit != null) {
                hit.selected = true;
                selNode = hit;
                // 标记拖拽
                dragNode = hit;
                dragOffX = hit.x;
                dragOffY = hit.y;
                dragMX = ev.x;
                dragMY = ev.y;
                // 阻止 GraphView 的平移拖拽（通过消费事件? 无法直接阻止，但 GraphView 检查 event.target==this，
                // 由于我们没有消费事件，event.target 是 GraphView，它会继续处理导致平移。）
                // 解决方案：手动调用 startDrag 覆盖 GraphView 的 drag
                // GraphView 的 startDrag 在 onMouseDown 中调用，我们在此之后触发，
                // 但我们的 listener 先于 GraphView 的 internal？不确定顺序。
                // 更好的方法：不依赖冲突，检测到命中节点时自己管理位置。
            } else {
                selNode = null;
                dragNode = null;
            }
        }
    }

    private void onGvDragUpdate(UIEvent ev) {
        // 节点拖拽
        if (dragNode != null) {
            float s = graphView.getScale();
            dragNode.x = dragOffX + (ev.x - dragMX) / s;
            dragNode.y = dragOffY + (ev.y - dragMY) / s;
        }
    }

    private void onGvDragEnd(UIEvent ev) {
        dragNode = null;
    }

    private void execCtx(int idx) {
        switch (idx) {
            case 0 -> runCmd("capturepoint create P" + (int)(Math.random()*1000) + ";");
            case 1 -> runCmd("capturepoint zone create Z" + (int)(Math.random()*1000) + ";");
            case 2 -> runCmd("capturepoint list");
            case 3 -> { Minecraft.getInstance().setScreen(null); new ControlPanelUI(level).open(); }
            case 4 -> Minecraft.getInstance().setScreen(null);
        }
    }

    // ================================================================
    //  数据模型
    // ================================================================

    enum NKind { POINT, ZONE }
    static class NodeData {
        String name; NKind kind; boolean owned; String info;
        float x, y, w = 160, h = 48; boolean selected;
        NodeData(String n, NKind k, boolean o, String i) { name = n; kind = k; owned = o; info = i; }
    }
    enum EKind { MEMBER, DEP }
    record EdgeData(NodeData f, NodeData t, EKind k) {}

    // ================================================================
    //  数据加载
    // ================================================================

    private void loadData() {
        var sl = getSL(); if (sl == null) return;
        var mgr = CaptureManager.get(sl);
        var pts = mgr.getPoints(); var zns = mgr.getZones();
        if (pts.isEmpty() && zns.isEmpty()) return;
        float sx = 80f, sy = 60f, gx = 200f, gy = 100f, cx = sx, cy = sy + 60f;
        int idx = 0;
        for (var e : pts.values()) { boolean o = e.owner() != null; var n = new NodeData(e.name(), NKind.POINT, o, o ? e.owner() : null); n.x = cx + 80; n.y = cy + 24; nodes.add(n); if (++idx % 4 == 0) { cx = sx; cy += gy; } else cx += gx; }
        cx = sx + 80f; cy += 60f; idx = 0;
        for (var e : zns.values()) { boolean c = mgr.isZoneCaptured(e.name()), a = mgr.canAccessZone(e.name()); var n = new NodeData(e.name(), NKind.ZONE, c, a ? null : e.requiredZone()); n.x = cx + 80; n.y = cy + 24; nodes.add(n); if (++idx % 4 == 0) { cx = sx + 80f; cy += gy; } else cx += gx; }
        for (var e : pts.values()) { var pn = find(e.name()); if (pn == null) continue; for (var ze : zns.values()) { if (ze.capturePoints().contains(e.name())) { var zn = find(ze.name()); if (zn != null) edges.add(new EdgeData(pn, zn, EKind.MEMBER)); } } }
        for (var e : zns.values()) { if (e.requiredZone() != null) { var zn = find(e.name()); var dn = find(e.requiredZone()); if (zn != null && dn != null) edges.add(new EdgeData(dn, zn, EKind.DEP)); } }
    }
    private NodeData find(String n) { for (var nd : nodes) if (nd.name.equals(n)) return nd; return null; }

    // ================================================================
    //  渲染层（纯绘制，不处理事件）
    // ================================================================

    private class GraphCanvas extends UIElement {
        @Override
        public void drawBackgroundAdditional(GUIContext ctx) {
            if (nodes.isEmpty()) {
                String msg = "Right-click for options";
                Font f = Minecraft.getInstance().font;
                DrawerHelper.drawText(ctx.graphics, msg, (getSizeWidth()-f.width(msg))/2f, getSizeHeight()/2f-f.lineHeight/2f, 1f, C_TXT_D);
                return;
            }
            for (var e : edges) drawEdge(ctx, e);
            for (var n : nodes) drawNode(ctx, n);
            if (selNode != null) drawInfo(ctx, selNode);
            if (ctxOpen) drawCtx(ctx);
        }

        private void drawNode(GUIContext ctx, NodeData n) {
            float rx = n.x - n.w/2f, ry = n.y - n.h/2f;
            int bg, bd;
            if (n.kind == NKind.POINT) { bg = n.owned ? C_PT_O_BG : C_PT_F_BG; bd = n.selected ? C_SEL : (n.owned ? C_PT_O_BD : C_PT_F_BD); }
            else if (n.owned) { bg = C_ZN_C_BG; bd = n.selected ? C_SEL : C_ZN_C_BD; }
            else if (n.info != null) { bg = C_ZN_L_BG; bd = n.selected ? C_SEL : C_ZN_L_BD; }
            else { bg = C_ZN_F_BG; bd = n.selected ? C_SEL : C_ZN_F_BD; }
            DrawerHelper.drawSolidRect(ctx.graphics, rx, ry, n.w, n.h, bg);
            if (n.selected) DrawerHelper.drawBorder(ctx.graphics, rx-2, ry-2, n.w+4, n.h+4, C_SEL, 2);
            DrawerHelper.drawBorder(ctx.graphics, rx, ry, n.w, n.h, bd, 1);
            String ic = n.kind == NKind.POINT ? (n.owned ? "\u2691" : "\u25CB") : (n.owned ? "\u25A0" : "\u25A1");
            Font f = Minecraft.getInstance().font;
            DrawerHelper.drawText(ctx.graphics, ic + " " + n.name, rx+8, ry+(n.h-f.lineHeight)/2f+1, 1f, C_TXT);
            String st = n.kind == NKind.POINT ? (n.owned ? n.info : "") : (n.owned ? "Cap" : (n.info != null ? "Lock" : "Free"));
            if (!st.isEmpty()) { float sw = f.width(st); DrawerHelper.drawText(ctx.graphics, st, rx+n.w-sw-6, ry+n.h-f.lineHeight-2, 0.7f, C_TXT_D); }
        }

        private void drawEdge(GUIContext ctx, EdgeData e) {
            float x1=e.f.x, y1=e.f.y+e.f.h/2f, x2=e.t.x, y2=e.t.y-e.t.h/2f;
            int c = e.k == EKind.MEMBER ? C_EDGE_M : C_EDGE_D;
            float w = e.k == EKind.MEMBER ? 1.5f : 2f, my = (y1+y2)/2f;
            var pts = new ArrayList<Vector2f>();
            for (int i = 0; i <= 24; i++) { float t = i/24f, t1 = 1-t; pts.add(new Vector2f(t1*t1*t1*x1+3*t1*t1*t*x1+3*t1*t*t*x2+t*t*t*x2, t1*t1*t1*y1+3*t1*t1*t*my+3*t1*t*t*my+t*t*t*y2)); }
            DrawerHelper.drawLines(ctx.graphics, pts, c, c, w);
            float dx=x2-x1, dy=y2-y1, l=(float)Math.sqrt(dx*dx+dy*dy); if (l<1f) return;
            float ux=dx/l, uy=dy/l, px=x2-ux*6f, py=y2-uy*6f, ax=-uy*8f*0.4f, ay=ux*8f*0.4f;
            var arr = new ArrayList<Vector2f>(); arr.add(new Vector2f(x2,y2)); arr.add(new Vector2f(px+ax,py+ay)); arr.add(new Vector2f(px-ax,py-ay));
            DrawerHelper.drawLines(ctx.graphics, arr, c, c, 1f);
        }

        private void drawInfo(GUIContext ctx, NodeData n) {
            String s = n.kind == NKind.POINT ? (n.owned ? "Owner: "+n.info : "Free point") : (n.owned ? "Captured" : (n.info != null ? "Requires: "+n.info : "Free zone"));
            DrawerHelper.drawText(ctx.graphics, s, n.x-n.w/2f, n.y+n.h/2f+4, 0.8f, C_TXT_D);
        }

        private void drawCtx(GUIContext ctx) {
            float mx = ctxMX, my = ctxMY, iw = 120f, ih = 16f, g = 1f, mh = 5 * (ih+g) + 4f;
            DrawerHelper.drawSolidRect(ctx.graphics, mx, my, iw, mh, 0xEE1E1E2E);
            DrawerHelper.drawBorder(ctx.graphics, mx, my, iw, mh, C_PNL_BD, 1);
            String[] ls = {"Create Point","Create Zone","List Status","\u21BB Refresh","\u2715 Close"};
            float iy = my+2; for (var l : ls) { DrawerHelper.drawText(ctx.graphics, l, mx+4, iy, 0.85f, C_TXT); iy += ih+g; }
        }
    }

    /**
     * 缩放滑块 — 自定义绘制 + 直接事件处理（非 contentChild，事件可到达）
     */
    private class Sld extends UIElement {
        private final GraphView gv;
        private final java.util.function.Supplier<Float> getVal;
        private final java.util.function.Consumer<Float> setVal;
        private float sSV; private float sMY; private boolean drag;

        Sld(GraphView gv, java.util.function.Supplier<Float> getVal, java.util.function.Consumer<Float> setVal) {
            this.gv = gv; this.getVal = getVal; this.setVal = setVal;
            addEventListener(UIEvents.MOUSE_DOWN, this::onDown);
            // DRAG 事件在 gv 层监听，因为滑块自身可能收不到 DRAG
            gv.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDrag);
            gv.addEventListener(UIEvents.DRAG_END, ev -> drag = false);
        }

        @Override
        public void drawBackgroundAdditional(GUIContext ctx) {
            float h = getSizeHeight(), w = getSizeWidth(), th = 16f, fh = h * getVal.get(), ty = h - fh - th / 2f;
            DrawerHelper.drawSolidRect(ctx.graphics, 0, 0, w, h, C_SLD_TK);
            DrawerHelper.drawSolidRect(ctx.graphics, 0, h - fh, w, fh, C_SLD_FL);
            DrawerHelper.drawSolidRect(ctx.graphics, 0, ty, w, th, C_SLD_TH);
        }

        private void onDown(UIEvent ev) {
            if (ev.button != 0) return;
            drag = true; sSV = getVal.get(); sMY = ev.y;
            float h = getSizeHeight(); if (h <= 0) return;
            float ly = screenYLocal(this, ev.y);
            setVal.accept(Mth.clamp(1f - ly / h, 0f, 1f));
        }

        private void onDrag(UIEvent ev) {
            if (!drag) return;
            float h = getSizeHeight(); if (h <= 0) return;
            float dy = (ev.y - sMY) / h;
            setVal.accept(Mth.clamp(sSV - dy, 0f, 1f));
        }
    }

    // ================================================================
    //  工具方法
    // ================================================================

    private net.minecraft.server.level.ServerLevel getSL() {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) return sl;
        var mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().getLevel(level.dimension());
        return null;
    }
    private static void runCmd(String c) { var p = Minecraft.getInstance().player; if (p == null) return; for (var s : c.split(";")) { s = s.trim(); if (!s.isEmpty()) p.connection.sendCommand(s); } }
}
