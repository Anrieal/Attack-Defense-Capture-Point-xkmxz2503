package com.xkmxz.attack_defense_capture_point_xkmxz.block.entity;

import com.xkmxz.attack_defense_capture_point_xkmxz.gui.ControlPanelUI;
import net.minecraft.core.BlockPos;
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
        if (level == null) return;
        new ControlPanelUI(level).open();
    }
}
