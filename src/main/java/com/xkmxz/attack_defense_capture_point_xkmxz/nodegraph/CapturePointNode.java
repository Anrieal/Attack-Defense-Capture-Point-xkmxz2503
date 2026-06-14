package com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import net.minecraft.network.chat.Component;

/**
 * 据点节点 - 代表一个占领点。
 * 具有一个输出端口（point_signal），可连接到 CaptureZoneNode 的输入端口。
 * 有一个可编辑的 owner 选项。
 */
public class CapturePointNode extends Node {

    public CapturePointNode() {
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("node.capture_point.display_name");
    }

    @Override
    public IGuiTexture getNodeIcon() {
        return new ColorRectTexture(0xFF66BB6A); // 绿色图标
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        // owner 选项 - 字符串类型
        context.addOption("owner", String.class)
                .withDefaultValue("")
                .withDisplayName(Component.translatable("node.capture_point.option.owner"))
                .build();
        // position 选项 - 只读坐标信息
        context.addOption("position", String.class)
                .withDefaultValue("0, 0, 0")
                .withDisplayName(Component.translatable("node.capture_point.option.position"))
                .build();
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        // 输出端口 - 据点信号
        context.addOutputPort("point_signal", CapturePointTypes.POINT_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_point.port.point_signal"))
                .build();
    }
}
