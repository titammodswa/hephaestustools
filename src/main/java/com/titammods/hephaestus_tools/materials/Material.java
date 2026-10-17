package com.titammods.hephaestus_tools.materials;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record Material(
        MaterialId id,
        MaterialIngredient ingredient,
        int color,
        MaterialStats headStats,
        MaterialStats handleStats,
        MaterialStats bindingStats
) {
    public record MaterialIngredient(Identifier id, boolean tag) {

        public static final MaterialIngredient EMPTY =
                new MaterialIngredient(Identifier.withDefaultNamespace("air"), false);

        private static final Codec<MaterialIngredient> STRING_CODEC = Codec.STRING.xmap(
                text -> text.startsWith("#")
                        ? new MaterialIngredient(Identifier.parse(text.substring(1)), true)
                        : new MaterialIngredient(Identifier.parse(text), false),
                value -> value.tag() ? "#" + value.id() : value.id().toString()
        );

        private static final Codec<MaterialIngredient> OBJECT_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Identifier.CODEC.optionalFieldOf("item")
                                .forGetter(value -> value.tag() ? Optional.<Identifier>empty() : Optional.of(value.id())),
                        Identifier.CODEC.optionalFieldOf("tag")
                                .forGetter(value -> value.tag() ? Optional.of(value.id()) : Optional.<Identifier>empty())
                ).apply(instance, (item, tag) -> tag
                        .map(found -> new MaterialIngredient(found, true))
                        .orElseGet(() -> new MaterialIngredient(item.orElse(EMPTY.id()), false)))
        );

        public static final Codec<MaterialIngredient> CODEC =
                Codec.either(STRING_CODEC, OBJECT_CODEC)
                        .xmap(either -> either.map(left -> left, right -> right), Either::left);

        public static final StreamCodec<RegistryFriendlyByteBuf, MaterialIngredient> STREAM_CODEC =
                StreamCodec.of(
                        (buf, value) -> {
                            buf.writeIdentifier(value.id());
                            buf.writeBoolean(value.tag());
                        },
                        buf -> new MaterialIngredient(buf.readIdentifier(), buf.readBoolean())
                );

        public boolean test(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return tag
                    ? stack.is(TagKey.create(Registries.ITEM, id))
                    : stack.is(BuiltInRegistries.ITEM.getValue(id));
        }
    }

    public static final Codec<Material> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MaterialId.CODEC.fieldOf("id").forGetter(Material::id),
                    MaterialIngredient.CODEC.optionalFieldOf("ingredient", MaterialIngredient.EMPTY)
                            .forGetter(Material::ingredient),
                    Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(Material::color),
                    MaterialStats.HEAD_CODEC.optionalFieldOf("head_stats", MaterialStats.EMPTY).forGetter(Material::headStats),
                    MaterialStats.HANDLE_CODEC.optionalFieldOf("handle_stats", MaterialStats.EMPTY_HANDLE).forGetter(Material::handleStats),
                    MaterialStats.BINDING_CODEC.optionalFieldOf("binding_stats", MaterialStats.EMPTY_BINDING).forGetter(Material::bindingStats)
            ).apply(instance, Material::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Material> STREAM_CODEC =
            StreamCodec.composite(
                    MaterialId.STREAM_CODEC, Material::id,
                    MaterialIngredient.STREAM_CODEC, Material::ingredient,
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
            default -> MaterialStats.emptyFor(slot);
        };
    }
}