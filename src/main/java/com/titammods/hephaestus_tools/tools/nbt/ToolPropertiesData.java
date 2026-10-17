package com.titammods.hephaestus_tools.tools.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.materials.tier.BuiltInTiers;
import com.titammods.hephaestus_tools.tools.stat.HarvestTier;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public record ToolPropertiesData(
        Map<String, Float> stats,
        int harvestTierOrdinal,
        Optional<Identifier> harvestTierId,
        List<String> activeTraits,
        int materialsHash
) {
    public static final ToolPropertiesData EMPTY = new ToolPropertiesData(
            Map.of(), 0, Optional.empty(), List.of(), 0
    );

    public static final Codec<ToolPropertiesData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                            .fieldOf("stats").forGetter(ToolPropertiesData::stats),
                    Codec.INT.optionalFieldOf("harvest_tier", 0).forGetter(ToolPropertiesData::harvestTierOrdinal),
                    Identifier.CODEC.optionalFieldOf("harvest_tier_id").forGetter(ToolPropertiesData::harvestTierId),
                    Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(ToolPropertiesData::activeTraits),
                    Codec.INT.optionalFieldOf("materials_hash", 0).forGetter(ToolPropertiesData::materialsHash)
            ).apply(instance, ToolPropertiesData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolPropertiesData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT),
                    ToolPropertiesData::stats,
                    ByteBufCodecs.VAR_INT, ToolPropertiesData::harvestTierOrdinal,
                    ByteBufCodecs.optional(Identifier.STREAM_CODEC), ToolPropertiesData::harvestTierId,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ToolPropertiesData::activeTraits,
                    ByteBufCodecs.VAR_INT, ToolPropertiesData::materialsHash,
                    ToolPropertiesData::new
            );

    public ToolPropertiesData withMaterialsHash(int hash) {
        return new ToolPropertiesData(stats, harvestTierOrdinal, harvestTierId, activeTraits, hash);
    }

    public float getStat(String key, float defaultValue) {
        return stats.getOrDefault(key, defaultValue);
    }

    public int getDurability() {
        return (int) getStat(ToolStats.DURABILITY, 1);
    }

    public float getMiningSpeed() {
        return getStat(ToolStats.MINING_SPEED, 1f);
    }

    public float getAttackDamage() {
        return getStat(ToolStats.ATTACK_DAMAGE, 1f);
    }

    public float getAttackSpeed() {
        return getStat(ToolStats.ATTACK_SPEED, 1f);
    }

    public int getEnchantability() {
        return (int) getStat(ToolStats.ENCHANTABILITY, 14f);
    }

    public HarvestTier getHarvestTier() {
        return harvestTierId.map(HarvestTier::of).orElseGet(() -> HarvestTier.byOrdinal(harvestTierOrdinal));
    }

    public boolean hasTrait(String traitId) {
        return activeTraits.contains(traitId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Float> stats = new HashMap<>();
        private int harvestTierOrdinal = 0;
        private Optional<Identifier> harvestTierId = Optional.empty();
        private List<String> traits = List.of();

        public Builder stat(String key, float value) {
            stats.put(key, value);
            return this;
        }

        public Builder durability(int value) {
            return stat(ToolStats.DURABILITY, value);
        }

        public Builder miningSpeed(float value) {
            return stat(ToolStats.MINING_SPEED, value);
        }

        public Builder attackDamage(float value) {
            return stat(ToolStats.ATTACK_DAMAGE, value);
        }

        public Builder attackSpeed(float value) {
            return stat(ToolStats.ATTACK_SPEED, value);
        }

        public Builder enchantability(int value) {
            return stat(ToolStats.ENCHANTABILITY, value);
        }

        public Builder harvestTier(HarvestTier tier) {
            OptionalInt position = BuiltInTiers.position(tier.id());
            this.harvestTierOrdinal = position.orElse(0);
            this.harvestTierId = position.isPresent() ? Optional.empty() : Optional.of(tier.id());
            return this;
        }

        public Builder traits(List<String> traits) {
            this.traits = traits;
            return this;
        }

        public ToolPropertiesData build() {
            return new ToolPropertiesData(Map.copyOf(stats), harvestTierOrdinal, harvestTierId, traits, 0);
        }
    }
}
