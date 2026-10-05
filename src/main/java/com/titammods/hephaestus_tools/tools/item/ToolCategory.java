package com.titammods.hephaestus_tools.tools.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public enum ToolCategory {
    PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE, null),
    AXE(BlockTags.MINEABLE_WITH_AXE, ItemAbilities.AXE_STRIP),
    SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL, ItemAbilities.SHOVEL_FLATTEN),
    HOE(BlockTags.MINEABLE_WITH_HOE, ItemAbilities.HOE_TILL),
    SWORD(BlockTags.SWORD_EFFICIENT, ItemAbilities.SWORD_SWEEP);

    public final TagKey<Block> tag;
    public final ItemAbility ability;

    ToolCategory(TagKey<Block> tag, ItemAbility ability) {
        this.tag = tag;
        this.ability = ability;
    }

    public boolean modifiesBlockOnUse() {
        return ability != null && ability != ItemAbilities.SWORD_SWEEP;
    }
}