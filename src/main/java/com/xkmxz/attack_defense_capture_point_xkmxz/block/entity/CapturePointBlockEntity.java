package com.xkmxz.attack_defense_capture_point_xkmxz.block.entity;

import com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph.CapturePointGraphScreen;
import net.minecraft.client.Minecraft;
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
        // 直接打开节点图编辑器
        Minecraft.getInstance().setScreen(null);
        new CapturePointGraphScreen(level).open();
    }
}
