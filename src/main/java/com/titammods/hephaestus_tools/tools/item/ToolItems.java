package com.titammods.hephaestus_tools.tools.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ToolItems {

    private ToolItems() {}

    public static class PickaxeItem extends ModifiableItem {
        public PickaxeItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_PICKAXE; }
    }

    public static class SledgeHammerItem extends ModifiableItem {
        public SledgeHammerItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_PICKAXE; }
    }

    public static class VeinHammerItem extends ModifiableItem {
        public VeinHammerItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_PICKAXE; }
    }

    public static class MattockItem extends ModifiableItem {
        public MattockItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_SHOVEL; }
    }

    public static class ExcavatorItem extends ModifiableItem {
        public ExcavatorItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_SHOVEL; }
    }

    public static class HandAxeItem extends ModifiableItem {
        public HandAxeItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_AXE; }
    }

    public static class BroadAxeItem extends ModifiableItem {
        public BroadAxeItem(Properties props) { super(props); }
        @Override public TagKey<Block> getToolBlockTag() { return BlockTags.MINEABLE_WITH_AXE; }
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
