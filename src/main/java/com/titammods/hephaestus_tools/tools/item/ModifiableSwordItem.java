package com.titammods.hephaestus_tools.tools.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public abstract class ModifiableSwordItem extends ModifiableItem {

    public ModifiableSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public TagKey<Block> getToolBlockTag() {
        return BlockTags.SWORD_EFFICIENT;
    }
}
