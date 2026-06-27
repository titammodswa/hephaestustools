package com.titammods.hephaestus_tools.tools.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Tiers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ToolPropertiesData(
        Map<String, Float> stats,
        int harvestTierOrdinal,
        List<String> activeTraits
) {
    public static final ToolPropertiesData EMPTY = new ToolPropertiesData(
            Map.of(), Tiers.WOOD.ordinal(), List.of()
    );

    public static final Codec<ToolPropertiesData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
                            .fieldOf("stats").forGetter(ToolPropertiesData::stats),
                    Codec.INT.optionalFieldOf("harvest_tier", 0).forGetter(ToolPropertiesData::harvestTierOrdinal),
                    Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(ToolPropertiesData::activeTraits)
            ).apply(instance, ToolPropertiesData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolPropertiesData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT),
                    ToolPropertiesData::stats,
                    ByteBufCodecs.VAR_INT, ToolPropertiesData::harvestTierOrdinal,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ToolPropertiesData::activeTraits,
                    ToolPropertiesData::new
            );

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

    public Tiers getHarvestTier() {
        Tiers[] values = Tiers.values();
        int ordinal = harvestTierOrdinal;
        if (ordinal < 0 || ordinal >= values.length) return Tiers.WOOD;
        return values[ordinal];
    }

    public boolean hasTrait(String traitId) {
        return activeTraits.contains(traitId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Float> stats = new HashMap<>();
        private int harvestTierOrdinal = Tiers.WOOD.ordinal();
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

        public Builder harvestTier(Tiers tier) {
            this.harvestTierOrdinal = tier.ordinal();
            return this;
        }

        public Builder traits(List<String> traits) {
            this.traits = traits;
            return this;
        }

        public ToolPropertiesData build() {
            return new ToolPropertiesData(Map.copyOf(stats), harvestTierOrdinal, traits);
        }
    }
}
