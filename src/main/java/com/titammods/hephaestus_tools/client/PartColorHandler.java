package com.titammods.hephaestus_tools.client;

import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public final class PartColorHandler implements ItemColor {

    public static final PartColorHandler INSTANCE = new PartColorHandler();

    private static final int NO_TINT = 0xFFFFFFFF;

    private PartColorHandler() {}

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return NO_TINT;
        if (!(stack.getItem() instanceof ToolPartItem part)) return NO_TINT;

        MaterialId id = part.getMaterial(stack);
        if (id == null || id.isEmpty()) return NO_TINT;

        Material material = MaterialManager.getInstance().getMaterial(id);
        if (material == null) return NO_TINT;

        int color = material.color();
        if ((color & 0xFF000000) == 0) color |= 0xFF000000;
        return color;
    }
}
