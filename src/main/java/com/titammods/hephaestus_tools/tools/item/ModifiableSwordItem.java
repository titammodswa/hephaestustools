package com.titammods.hephaestus_tools.tools.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ModifiableSwordItem extends ModifiableItem {

    public ModifiableSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public TagKey<Block> getToolBlockTag() {
        return BlockTags.SWORD_EFFICIENT;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return state.is(getToolBlockTag());
    }
}