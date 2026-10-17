package com.titammods.hephaestus_tools.tools.stat;

import com.titammods.hephaestus_tools.materials.tier.BuiltInTiers;
import com.titammods.hephaestus_tools.materials.tier.TierDefinition;
import com.titammods.hephaestus_tools.materials.tier.TierManager;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record HarvestTier(Identifier id, TierDefinition definition) {

    public int miningLevel() {
        return definition.miningLevel();
    }

    public TagKey<Block> incorrectForTag() {
        return definition.incorrectFor();
    }

    public boolean canHarvest(BlockState state) {
        return !state.is(definition.incorrectFor());
    }

    public String nameKey() {
        return "tooltip." + id.getNamespace() + ".tier." + id.getPath();
    }

    public static HarvestTier of(Identifier id) {
        return definitionOf(id)
                .map(definition -> new HarvestTier(id, definition))
                .orElseGet(() -> new HarvestTier(BuiltInTiers.WOOD, definitionOf(BuiltInTiers.WOOD).orElseThrow()));
    }

    public static HarvestTier byOrdinal(int ordinal) {
        return of(BuiltInTiers.byPosition(ordinal));
    }

    private static Optional<TierDefinition> definitionOf(Identifier id) {
        return TierManager.getInstance().get(id).or(() -> BuiltInTiers.get(id));
    }
}
