package com.titammods.hephaestus_tools.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.titammods.block.TableBlockEntity;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.registry.ModComponents;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TableBlockEntity.class, remap = false)
public class TableBlockEntityMixin {

    private static final String MOLTEN_PREFIX = "molten_";

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lcom/titammods/setup/ModRecipes$CastingTableRecipe;result()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack hephaestusTools$applyPartMaterial(ItemStack result, @Local FluidStack currentFluid) {
        if (!(result.getItem() instanceof ToolPartItem)) return result;
        if (currentFluid == null || currentFluid.isEmpty()) return result;

        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(currentFluid.getFluid());
        if (fluidId == null) return result;

        String path = fluidId.getPath();
        if (path.startsWith(MOLTEN_PREFIX)) path = path.substring(MOLTEN_PREFIX.length());

        ItemStack copy = result.copy();
        copy.set(ModComponents.PART_MATERIAL.get(), MaterialId.of(HephaestusTools.MOD_ID, path));
        return copy;
    }
}