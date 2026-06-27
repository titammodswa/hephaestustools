package com.titammods.hephaestus_tools.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.registry.ModRecipes;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record PartRecipe(
        ResourceLocation resultPart,
        ResourceLocation patternItem,
        int materialCost
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
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resultPart);
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public ItemStack createResult(MaterialId material) {
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resultPart);
        if (item instanceof ToolPartItem partItem) {
            return partItem.withMaterial(material);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<PartRecipe> getSerializer() {
        return ModRecipes.PART_BUILDER_SERIALIZER.get();
    }

    @Override
    public RecipeType<PartRecipe> getType() {
        return ModRecipes.PART_BUILDER.get();
    }

    public static class Serializer implements RecipeSerializer<PartRecipe> {

        public static final MapCodec<PartRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("result_part").forGetter(PartRecipe::resultPart),
                        ResourceLocation.CODEC.fieldOf("pattern").forGetter(PartRecipe::patternItem),
                        net.minecraft.util.ExtraCodecs.POSITIVE_INT
                                .optionalFieldOf("material_cost", 1).forGetter(PartRecipe::materialCost)
                ).apply(instance, PartRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, PartRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, PartRecipe::resultPart,
                        ResourceLocation.STREAM_CODEC, PartRecipe::patternItem,
                        ByteBufCodecs.VAR_INT, PartRecipe::materialCost,
                        PartRecipe::new
                );

        @Override
        public MapCodec<PartRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PartRecipe> streamCodec() { return STREAM_CODEC; }
    }
}