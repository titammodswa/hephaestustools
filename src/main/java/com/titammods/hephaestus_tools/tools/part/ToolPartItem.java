package com.titammods.hephaestus_tools.tools.part;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.registry.ModComponents;
import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.trait.ToolTraits;
import com.titammods.hephaestus_tools.materials.trait.ToolTraits.TraitInstance;
import com.titammods.hephaestus_tools.tools.helper.ToolTooltipBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ToolPartItem extends Item {

    private final int partSlot;

    public ToolPartItem(Properties props, int partSlot) {
        super(props);
        this.partSlot = partSlot;
    }

    public int getPartSlot() {
        return partSlot;
    }

    public MaterialId getMaterial(ItemStack stack) {
        MaterialId mat = stack.get(ModComponents.PART_MATERIAL.get());
        return mat == null ? MaterialId.EMPTY : mat;
    }

    public ItemStack withMaterial(MaterialId material) {
        ItemStack stack = new ItemStack(this);
        stack.set(ModComponents.PART_MATERIAL.get(), material);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        MaterialId mat = getMaterial(stack);
        if (mat.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.hephaestus_tools.part.no_material")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        Material material = MaterialManager.getInstance().getMaterial(mat);
        int color = material != null ? material.color() & 0xFFFFFF : 0xFFFFFF;
        tooltip.accept(Component.translatable("tooltip.hephaestus_tools.material",
                        Component.translatable("material.hephaestus_tools." + mat.id().getPath())
                                .withStyle(s -> s.withColor(color)))
                .withStyle(ChatFormatting.GRAY));

        boolean shift = flag.hasShiftDown();
        for (TraitInstance trait : ToolTraits.ofPart(mat, partSlot)) {
            tooltip.accept(ToolTooltipBuilder.traitName(trait)
                    .withStyle(ChatFormatting.DARK_AQUA));
            if (shift) {
                tooltip.accept(Component.literal("  ")
                        .append(Component.translatable(ToolTraits.descriptionKey(trait.id()))
                                .withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        if (shift) {
            tooltip.accept(Component.empty());
            ToolTooltipBuilder.partStats(mat, partSlot).forEach(tooltip);
        } else {
            tooltip.accept(Component.empty());
            tooltip.accept(ToolTooltipBuilder.holdShift());
        }
    }
}