package com.titammods.hephaestus_tools.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.registry.ModRecipes;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
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

import java.util.ArrayList;
import java.util.List;

public record ToolBuildingRecipe(
        ResourceLocation result,
        List<ResourceLocation> parts
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
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(result);
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

    public List<MaterialId> extractMaterials(List<ItemStack> inputParts) {
        if (inputParts.size() < parts.size()) return null;
        List<MaterialId> materials = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            ItemStack stack = inputParts.get(i);
            if (stack.isEmpty()) return null;
            if (!(stack.getItem() instanceof ToolPartItem partItem)) return null;
            ResourceLocation expectedPart = parts.get(i);
            ResourceLocation actualPart = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(partItem);
            if (!expectedPart.equals(actualPart)) return null;
            MaterialId mat = partItem.getMaterial(stack);
            if (mat.isEmpty()) return null;
            materials.add(mat);
        }
        return materials;
    }

    public ItemStack createResult(List<MaterialId> materials) {
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(result);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return ToolStack.createTool(new ItemStack(item), materials);
    }

    @Override
    public RecipeSerializer<ToolBuildingRecipe> getSerializer() {
        return ModRecipes.TOOL_BUILDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<ToolBuildingRecipe> getType() {
        return ModRecipes.TOOL_BUILDING.get();
    }

    public static class Serializer implements RecipeSerializer<ToolBuildingRecipe> {

        public static final MapCodec<ToolBuildingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("result").forGetter(ToolBuildingRecipe::result),
                        ResourceLocation.CODEC.listOf().fieldOf("parts").forGetter(ToolBuildingRecipe::parts)
                ).apply(instance, ToolBuildingRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ToolBuildingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, ToolBuildingRecipe::result,
                        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), ToolBuildingRecipe::parts,
                        ToolBuildingRecipe::new
                );

        @Override
        public MapCodec<ToolBuildingRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolBuildingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}