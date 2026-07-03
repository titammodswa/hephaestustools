package com.titammods.hephaestus_tools.compat.jei;

import com.titammods.compat.jei.TitamModsJEIPlugin;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.registry.ModItems;
import com.titammods.setup.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

@JeiPlugin
public class HephaestusToolsJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(HephaestusTools.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jei) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        var rm = level.getRecipeManager();
        IRecipeManager jeiRm = jei.getRecipeManager();

        Set<Item> partItems = Set.of(
                ModItems.PICK_HEAD.get(), ModItems.HAMMER_HEAD.get(), ModItems.SMALL_AXE_HEAD.get(),
                ModItems.BROAD_AXE_HEAD.get(), ModItems.ADZE_HEAD.get(), ModItems.LARGE_PLATE.get(),
                ModItems.SMALL_BLADE.get(), ModItems.LARGE_BLADE.get(), ModItems.TOOL_HANDLE.get(),
                ModItems.TOUGH_HANDLE.get(), ModItems.TOOL_BINDING.get(), ModItems.TOUGH_BINDING.get());

        List<ModRecipes.CastingTableRecipe> partCasting = rm.getAllRecipesFor(ModRecipes.CASTING_TABLE_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(r -> partItems.contains(r.result().getItem()))
                .toList();
        if (!partCasting.isEmpty()) {
            jeiRm.hideRecipes(TitamModsJEIPlugin.CASTING_TABLE_TYPE, partCasting);
        }

        List<ModRecipes.MeltingRecipe> partMelting = rm.getAllRecipesFor(ModRecipes.MELTING_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(this::isPartMelting)
                .toList();
        if (!partMelting.isEmpty()) {
            jeiRm.hideRecipes(TitamModsJEIPlugin.MELTING_TYPE, partMelting);
            jeiRm.hideRecipes(TitamModsJEIPlugin.SMELTERY_TYPE, partMelting);
        }
    }

    private boolean isPartMelting(ModRecipes.MeltingRecipe recipe) {
        for (ItemStack stack : recipe.input().getItems()) {
            if (stack.getItem() instanceof com.titammods.hephaestus_tools.tools.part.ToolPartItem) return true;
        }
        return false;
    }
}