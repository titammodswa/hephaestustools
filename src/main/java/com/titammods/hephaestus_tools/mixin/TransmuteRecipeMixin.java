package com.titammods.hephaestus_tools.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TransmuteRecipe.class)
public abstract class TransmuteRecipeMixin {

    @ModifyReturnValue(method = "createWithOriginalComponents(Lnet/minecraft/world/item/ItemStackTemplate;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private static ItemStack hephaestusTools$rebuildTool(ItemStack result) {
        ToolStack.refresh(result);
        return result;
    }
}
