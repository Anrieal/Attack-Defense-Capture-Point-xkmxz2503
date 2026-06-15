package com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import net.minecraft.network.chat.Component;

/**
 * 判断器节点 — 根据条件将信号分配到不同输出路径。<br>
 * <br>
 * <b>据点信号路由：</b><br>
 * 接收 POINT_SIGNAL（target），根据条件输出到 true_out / false_out（均为 POINT_SIGNAL），<br>
 * 用于将据点按条件分配到不同的区域。<br>
 * <br>
 * <b>区域信号路由：</b><br>
 * 接收 ZONE_SIGNAL（zone_target），根据条件输出到 zone_true_out / zone_false_out（均为 ZONE_SIGNAL），<br>
 * 用于控制区域间的依赖解锁关系。<br>
 * <br>
 * <b>条件类型 (condition)：</b>
 * <ul>
 *   <li>{@code captured} — captured 为 true</li>
 *   <li>{@code not_captured} — captured 为 false</li>
 *   <li>{@code owner_team} — ownerTeam 匹配 target_team</li>
 *   <li>{@code capturing} — capturingTeam 匹配 target_team（仅据点）</li>
 *   <li>{@code not_capturing} — capturingTeam 为 null（仅据点）</li>
 * </ul>
 */
public class CaptureDecisionNode extends Node {

    public CaptureDecisionNode() {
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("node.capture_decision.display_name");
    }

    @Override
    public IGuiTexture getNodeIcon() {
        return new ColorRectTexture(0xFFFF9800); // 橙色图标
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);

        // condition 判断条件类型
        context.addOption("condition", String.class)
                .withDefaultValue("captured")
                .withDisplayName(Component.translatable("node.capture_decision.option.condition"))
                .build();

        // target_team 目标队伍（用于 owner_team / capturing 判断）
        context.addOption("target_team", String.class)
                .withDefaultValue("")
                .withDisplayName(Component.translatable("node.capture_decision.option.target_team"))
                .build();

        // progress_threshold 进度阈值（预留，用于 future 进度比较）
        context.addOption("progress_threshold", Integer.class)
                .withDefaultValue(50)
                .withDisplayName(Component.translatable("node.capture_decision.option.progress_threshold"))
                .build();
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);

        // ---- 据点信号端口 ----

        // 输入端口 - 接收据点信号
        context.addInputPort("target", CapturePointTypes.POINT_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_decision.port.target"))
                .build();

        // 输出端口 - 条件满足时据点信号从此输出
        context.addOutputPort("true_out", CapturePointTypes.POINT_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_decision.port.true_out"))
                .build();

        // 输出端口 - 条件不满足时据点信号从此输出
        context.addOutputPort("false_out", CapturePointTypes.POINT_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_decision.port.false_out"))
                .build();

        // ---- 区域信号端口 ----

        // 输入端口 - 接收区域依赖信号（来自 zone_out）
        context.addInputPort("zone_target", CapturePointTypes.ZONE_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_decision.port.zone_target"))
                .build();

        // 输出端口 - 条件满足时区域依赖信号从此输出（连接至 required_zone）
        context.addOutputPort("zone_true_out", CapturePointTypes.ZONE_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_decision.port.zone_true_out"))
                .build();

        // 输出端口 - 条件不满足时区域依赖信号从此输出（连接至 required_zone）
        context.addOutputPort("zone_false_out", CapturePointTypes.ZONE_SIGNAL)
                .withDisplayName(Component.translatable("node.capture_decision.port.zone_false_out"))
                .build();
    }
}
