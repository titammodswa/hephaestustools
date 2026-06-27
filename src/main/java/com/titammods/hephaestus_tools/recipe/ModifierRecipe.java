package com.titammods.hephaestus_tools.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ModifierRecipe(
        ResourceLocation modifierId,
        List<Ingredient> inputs,
        int maxLevel,
        int slotsRequired,
        String slotType,
        Optional<Ingredient> toolRequirement
) implements Recipe<RecipeInput> {

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<ModifierRecipe> getSerializer() {
        return ModRecipes.MODIFIER_SERIALIZER.get();
    }

    @Override
    public RecipeType<ModifierRecipe> getType() {
        return ModRecipes.MODIFIER.get();
    }

    public static class Serializer implements RecipeSerializer<ModifierRecipe> {

        public static final MapCodec<ModifierRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("modifier").forGetter(ModifierRecipe::modifierId),
                        Ingredient.CODEC.listOf().fieldOf("inputs").forGetter(ModifierRecipe::inputs),
                        net.minecraft.util.ExtraCodecs.POSITIVE_INT
                                .optionalFieldOf("max_level", 1).forGetter(ModifierRecipe::maxLevel),
                        net.minecraft.util.ExtraCodecs.POSITIVE_INT
                                .optionalFieldOf("slots_required", 1).forGetter(ModifierRecipe::slotsRequired),
                        net.minecraft.util.ExtraCodecs.NON_EMPTY_STRING
                                .optionalFieldOf("slot_type", "upgrade").forGetter(ModifierRecipe::slotType),
                        Ingredient.CODEC.optionalFieldOf("tool_requirement")
                                .forGetter(ModifierRecipe::toolRequirement)
                ).apply(instance, ModifierRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ModifierRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, r) -> {
                            buf.writeResourceLocation(r.modifierId());
                            buf.writeVarInt(r.inputs().size());
                            for (Ingredient ing : r.inputs()) {
                                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                            }
                            buf.writeVarInt(r.maxLevel());
                            buf.writeVarInt(r.slotsRequired());
                            buf.writeUtf(r.slotType());
                            buf.writeBoolean(r.toolRequirement().isPresent());
                            r.toolRequirement().ifPresent(ing ->
                                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing));
                        },
                        buf -> {
                            ResourceLocation id = buf.readResourceLocation();
                            int count = buf.readVarInt();
                            List<Ingredient> inputs = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                            }
                            int maxLevel = buf.readVarInt();
                            int slots = buf.readVarInt();
                            String slotType = buf.readUtf();
                            Optional<Ingredient> req = buf.readBoolean()
                                    ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buf))
                                    : Optional.empty();
                            return new ModifierRecipe(id, inputs, maxLevel, slots, slotType, req);
                        }
                );

        @Override
        public MapCodec<ModifierRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ModifierRecipe> streamCodec() { return STREAM_CODEC; }
    }
}