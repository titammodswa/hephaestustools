package com.titammods.hephaestus_tools.client;

import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public final class ToolColorHandler implements ItemColor {

    public static final ToolColorHandler INSTANCE = new ToolColorHandler();

    private static final int NO_TINT = 0xFFFFFFFF;

    private ToolColorHandler() {}

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex < 0) {
            return NO_TINT;
        }

        if (!ToolStack.isInitialized(stack)) {
            return NO_TINT;
        }

        int materialIndex = ToolLayerMap.materialIndexForLayer(stack.getItem(), tintIndex);
        if (materialIndex < 0) {
            return NO_TINT;
        }

        MaterialId materialId = ToolStack.getMaterial(stack, materialIndex);
        if (materialId == null || materialId.isEmpty()) {
            return NO_TINT;
        }

        Material material = MaterialManager.getInstance().getMaterial(materialId);
        if (material == null) {
            return NO_TINT;
        }

        int color = material.color();
        if ((color & 0xFF000000) == 0) {
            color |= 0xFF000000;
        }
        return color;
    }
}