package com.xkmxz.attack_defense_capture_point_xkmxz.block.entity;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.xkmxz.attack_defense_capture_point_xkmxz.gui.ControlPanelUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CapturePointBlockEntity extends BlockEntity {

    public static BlockEntityType<CapturePointBlockEntity> TYPE;

    public CapturePointBlockEntity(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    public void openUI(Player player) {
        var root = new UIElement()
                .layout(layout -> layout.paddingAll(7).gapAll(5).width(350).height(200))
                .style(style -> style.background(Sprites.BORDER));

        var pos = worldPosition;
        root.addChildren(
                new Label().setText(Component.translatable("block.attack_defense_capture_point_xkmxz.capture_point_block")),
                new Label().setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.block.info",
                        pos.getX(), pos.getY(), pos.getZ())),

                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.block.create_here"))
                        .setOnClick(event -> {
                            var p = Minecraft.getInstance().player;
                            if (p != null) {
                                p.connection.sendCommand("capturepoint create " + pos.getX() + "_" + pos.getZ());
                                Minecraft.getInstance().setScreen(null);
                            }
                        }),

                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.block.list_status"))
                        .setOnClick(event -> {
                            var p = Minecraft.getInstance().player;
                            if (p != null) {
                                p.connection.sendCommand("capturepoint list");
                            }
                        }),

                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.block.open_control_panel"))
                        .setOnClick(event -> {
                            if (level != null) {
                                Minecraft.getInstance().setScreen(null);
                                new ControlPanelUI(level).open();
                            }
                        }),

                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.block.close"))
                        .setOnClick(event -> Minecraft.getInstance().setScreen(null))
        );

        ModularUI ui = ModularUI.of(UI.of(root));
        Minecraft.getInstance().setScreen(
                new ModularUIScreen(ui, Component.translatable("block.attack_defense_capture_point_xkmxz.capture_point_block"))
        );
    }
}
