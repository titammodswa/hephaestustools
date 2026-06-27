package com.titammods.hephaestus_tools.registry;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.recipe.PartRecipe;
import com.titammods.hephaestus_tools.recipe.ToolBuildingRecipe;
import com.titammods.hephaestus_tools.recipe.ModifierRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, HephaestusTools.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, HephaestusTools.MOD_ID);

    public static final Supplier<RecipeType<PartRecipe>> PART_BUILDER =
            RECIPE_TYPES.register("part_builder",
                    () -> RecipeType.simple(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    HephaestusTools.MOD_ID, "part_builder")));

    public static final Supplier<RecipeType<ToolBuildingRecipe>> TOOL_BUILDING =
            RECIPE_TYPES.register("tool_building",
                    () -> RecipeType.simple(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    HephaestusTools.MOD_ID, "tool_building")));

    public static final Supplier<RecipeType<ModifierRecipe>> MODIFIER =
            RECIPE_TYPES.register("modifier",
                    () -> RecipeType.simple(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                    HephaestusTools.MOD_ID, "modifier")));

    public static final Supplier<RecipeSerializer<PartRecipe>> PART_BUILDER_SERIALIZER =
            RECIPE_SERIALIZERS.register("part_builder", PartRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<ToolBuildingRecipe>> TOOL_BUILDING_SERIALIZER =
            RECIPE_SERIALIZERS.register("tool_building", ToolBuildingRecipe.Serializer::new);

    public static final Supplier<RecipeSerializer<ModifierRecipe>> MODIFIER_SERIALIZER =
            RECIPE_SERIALIZERS.register("modifier", ModifierRecipe.Serializer::new);

    public static void registerRecipeTypes() {
    }
}
