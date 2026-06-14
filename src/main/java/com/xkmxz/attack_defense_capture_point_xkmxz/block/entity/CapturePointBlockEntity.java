package com.xkmxz.attack_defense_capture_point_xkmxz.block.entity;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
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
                .layout(layout -> layout.paddingAll(7).gapAll(5))
                .style(style -> style.background(Sprites.BORDER));

        root.addChildren(
                new Label().setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.capture_point.title")),
                new Label().setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.capture_point.info")),
                new Button()
                        .setText(Component.translatable("gui.attack_defense_capture_point_xkmxz.capture_point.test_button"))
                        .setOnClick(event -> {
                            Minecraft.getInstance().player.sendSystemMessage(
                                    Component.translatable("gui.attack_defense_capture_point_xkmxz.capture_point.button_clicked"));
                        })
        );

        ModularUI ui = ModularUI.of(UI.of(root));

        Minecraft.getInstance().setScreen(
                new ModularUIScreen(ui, Component.translatable("gui.attack_defense_capture_point_xkmxz.capture_point.title"))
        );
    }
}
