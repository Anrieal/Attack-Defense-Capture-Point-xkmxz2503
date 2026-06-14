package com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.GraphCommands.DeleteElementsCommand;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 据点管理图视图 - 使用 LDLib2 nodegraphtookit 框架提供节点图编辑体验。
 * 右键菜单支持创建据点节点、创建区域节点、删除节点、查看状态、刷新等操作。
 */
public class CapturePointGraphView extends GraphView {

    private int pendingViewportResetTicks;

    public CapturePointGraphView() {
        super();
        // 隐藏默认的标题栏和面板层
        hideHeaders();
        hidePanels();
        // 抑制默认的节点库显示
        suppressDefaultItemLibrary();
    }

    private void hideHeaders() {
        header.setVisible(false);
        header.setDisplay(false);
        header.layout(l -> l.width(0).height(0));
    }

    private void hidePanels() {
        getPanelLayer().clearAllChildren();
        getPanelLayer().setVisible(false);
        getPanelLayer().setDisplay(false);
    }

    private void suppressDefaultItemLibrary() {
        itemLibrary.setVisible(false);
        itemLibrary.setDisplay(false);
        itemLibrary.setAllowHitTest(false);
        itemLibrary.layout(l -> {
            l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE);
            l.left(-100000f);
            l.top(-100000f);
            l.width(0);
            l.height(0);
        });
    }

    @Override
    public CapturePointGraphView loadGraph(Graph graph) {
        super.loadGraph(graph);
        suppressDefaultItemLibrary();
        pendingViewportResetTicks = 1;
        return this;
    }

    @Override
    public void screenTick() {
        hideDefaultItemLibraryIfDisplayed();
        super.screenTick();
        hideDefaultItemLibraryIfDisplayed();

        if (pendingViewportResetTicks > 0) {
            if (graphView.getContentWidth() > 0 && graphView.getContentHeight() > 0) {
                pendingViewportResetTicks--;
                if (pendingViewportResetTicks == 0) {
                    resetViewportTransform();
                }
            }
        }
    }

    private void hideDefaultItemLibraryIfDisplayed() {
        if (itemLibrary.isDisplayed()) {
            itemLibrary.hide();
        }
        suppressDefaultItemLibrary();
    }

    private void resetViewportTransform() {
        graphView.fit(0, 0, graphView.getContentWidth(), graphView.getContentHeight(), 1f);
    }

    @Override
    protected TreeBuilder.Menu createMenu(float screenX, float screenY) {
        var menu = TreeBuilder.Menu.start();

        // 创建据点节点
        menu.leaf(
                Component.translatable("gui.capture_point_graph.menu.create_point").getString(),
                () -> {
                    float wx = (screenX - graphView.getPositionX()) / graphView.getScale() + graphView.getOffsetX();
                    float wy = (screenY - graphView.getPositionY()) / graphView.getScale() + graphView.getOffsetY();
                    getGraph().graphModel.createNodeModel(
                            new CapturePointNode(), new org.joml.Vector2f(wx, wy));
                });

        // 创建区域节点
        menu.leaf(
                Component.translatable("gui.capture_point_graph.menu.create_zone").getString(),
                () -> {
                    float wx = (screenX - graphView.getPositionX()) / graphView.getScale() + graphView.getOffsetX();
                    float wy = (screenY - graphView.getPositionY()) / graphView.getScale() + graphView.getOffsetY();
                    getGraph().graphModel.createNodeModel(
                            new CaptureZoneNode(), new org.joml.Vector2f(wx, wy));
                });

        // 如果有选中的节点，添加删除选项
        if (!getSelected().isEmpty()) {
            boolean hasNode = getSelected().stream().anyMatch(m -> m instanceof AbstractNodeModel);
            if (hasNode) {
                menu.leaf(
                        Component.translatable("gui.capture_point_graph.menu.delete").getString(),
                        () -> {
                            List<GraphElementModel> toDelete = getSelected().stream()
                                    .filter(m -> m instanceof GraphElementModel)
                                    .map(m -> (GraphElementModel) m)
                                    .toList();
                            if (!toDelete.isEmpty()) {
                                dispatchCommand(new DeleteElementsCommand(toDelete));
                            }
                        });
            }
        }

        // 查看状态
        menu.leaf(Component.translatable("gui.capture_point_graph.menu.list_status").getString(), () -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.connection.sendCommand("capturepoint list");
            }
        });

        // 刷新
        menu.leaf(Component.translatable("gui.capture_point_graph.menu.refresh").getString(), () -> {
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level != null) {
                mc.setScreen(null);
                new CapturePointGraphScreen(level).open();
            }
        });

        // 关闭
        menu.leaf(Component.translatable("gui.capture_point_graph.menu.close").getString(), () -> {
            Minecraft.getInstance().setScreen(null);
        });

        return menu;
    }
}
