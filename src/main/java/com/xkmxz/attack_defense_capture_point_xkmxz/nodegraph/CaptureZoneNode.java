package com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import net.minecraft.network.chat.Component;

/**
 * 区域节点 - 代表一个占领区域。
 * 具有一个输入端口（point_in）接收来自 CapturePointNode 的连接，
 * 以及一个输入端口（required_zone）接收来自其他区域的依赖连接。
 */
public class CaptureZoneNode extends Node {

    public CaptureZoneNode() {
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("node.capture_zone.display_name");
    }

    @Override
    public IGuiTexture getNodeIcon() {
        return new ColorRectTexture(0xFF42A5F5); // 蓝色图标
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        // captured 状态选项（只读）
        context.addOption("captured", Boolean.class)
                .withDefaultValue(false)
                .withDisplayName(Component.translatable("node.capture_zone.option.captured"))
                .build();
        // requiredZone 依赖区域名称
        context.addOption("required_zone", String.class)
                .withDefaultValue("")
                .withDisplayName(Component.translatable("node.capture_zone.option.required_zone"))
                .build();
        // 包含的据点列表（只读展示用）
        context.addOption("points", String.class)
                .withDefaultValue("")
                .withDisplayName(Component.translatable("node.capture_zone.option.points"))
                .build();
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        // 输入端口 - 接收据点信号（多个据点可通过此端口加入区域）
        context.addInputPort("point_in", CapturePointTypes.POINT_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_zone.port.point_in"))
                .build();
        // 输入端口 - 接收区域依赖信号（该区域依赖的另一个区域）
        context.addInputPort("required_zone", CapturePointTypes.ZONE_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_zone.port.required_zone"))
                .build();
    }
}
