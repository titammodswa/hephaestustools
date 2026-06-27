package com.titammods.hephaestus_tools.materials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

public record Material(
        MaterialId id,
        Ingredient ingredient,
        int color,
        MaterialStats headStats,
        MaterialStats handleStats,
        MaterialStats bindingStats
) {
    public static final Codec<Material> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MaterialId.CODEC.fieldOf("id").forGetter(Material::id),
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(Material::ingredient),
                    Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(Material::color),
                    MaterialStats.CODEC.optionalFieldOf("head_stats", MaterialStats.EMPTY).forGetter(Material::headStats),
                    MaterialStats.CODEC.optionalFieldOf("handle_stats", MaterialStats.EMPTY).forGetter(Material::handleStats),
                    MaterialStats.CODEC.optionalFieldOf("binding_stats", MaterialStats.EMPTY).forGetter(Material::bindingStats)
            ).apply(instance, Material::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Material> STREAM_CODEC =
            StreamCodec.composite(
                    MaterialId.STREAM_CODEC,      Material::id,
                    Ingredient.CONTENTS_STREAM_CODEC, Material::ingredient,
                    StreamCodec.of(
                            (buf, color) -> buf.writeInt(color),
                            buf -> buf.readInt()
                    ), Material::color,
                    MaterialStats.STREAM_CODEC, Material::headStats,
                    MaterialStats.STREAM_CODEC, Material::handleStats,
                    MaterialStats.STREAM_CODEC, Material::bindingStats,
                    Material::new
            );

    public MaterialStats getStatsForSlot(int slot) {
        return switch (slot) {
            case 0  -> headStats;
            case 1  -> handleStats;
            case 2  -> bindingStats;
            default -> MaterialStats.EMPTY;
        };
    }
}
