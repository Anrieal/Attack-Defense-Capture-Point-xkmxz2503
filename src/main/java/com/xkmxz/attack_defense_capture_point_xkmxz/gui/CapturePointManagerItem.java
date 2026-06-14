package com.xkmxz.attack_defense_capture_point_xkmxz.gui;

import com.xkmxz.attack_defense_capture_point_xkmxz.nodegraph.CapturePointGraphScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CapturePointManagerItem extends Item {

    public CapturePointManagerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            new CapturePointGraphScreen(level).open();
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
