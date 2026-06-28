package com.titammods.hephaestus_tools.tools.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.titammods.hephaestus_tools.tools.aoe.IAoeTool;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;

public final class ToolItems {

    private ToolItems() {}

    public static class PickaxeItem extends ModifiableItem {
        public PickaxeItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_PICKAXE; }
    }

    public static class SledgeHammerItem extends ModifiableItem
            implements com.titammods.hephaestus_tools.tools.aoe.IAoeTool {
        public SledgeHammerItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_PICKAXE; }
    }

    public static class VeinHammerItem extends ModifiableItem implements IAoeTool {
        public VeinHammerItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_PICKAXE; }
        @Override
        public List<BlockPos> getExtraBlocks(Level world, BlockHitResult rt, Player player, ItemStack stack) {
            return veinExtraBlocks(world, rt, player, stack, 2);
        }
    }

    public static class MattockItem extends ModifiableItem {
        public MattockItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_SHOVEL; }
    }

    public static class ExcavatorItem extends ModifiableItem implements IAoeTool {
        public ExcavatorItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_SHOVEL; }
    }

    public static class HandAxeItem extends ModifiableItem {
        public HandAxeItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_AXE; }
    }

    public static class BroadAxeItem extends ModifiableItem implements IAoeTool {
        public BroadAxeItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_AXE; }
        @Override
        public List<BlockPos> getExtraBlocks(Level world, BlockHitResult rt, Player player, ItemStack stack) {
            if (rt != null && rt.getType() == HitResult.Type.BLOCK
                    && world.getBlockState(rt.getBlockPos()).is(BlockTags.LOGS)) {
                return treeExtraBlocks(world, rt, player, stack);
            }
            return IAoeTool.super.getExtraBlocks(world, rt, player, stack);
        }
    }

    public static class KamaItem extends ModifiableItem {
        public KamaItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_HOE; }
    }

    public static class ScytheItem extends ModifiableItem {
        public ScytheItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_HOE; }
    }

    public static class DaggerItem extends ModifiableSwordItem {
        public DaggerItem(Properties props) { super(props); }
    }

    public static class SwordItem extends ModifiableSwordItem {
        public SwordItem(Properties props) { super(props); }
    }

    public static class CleaverItem extends ModifiableSwordItem {
        public CleaverItem(Properties props) { super(props); }
    }
}