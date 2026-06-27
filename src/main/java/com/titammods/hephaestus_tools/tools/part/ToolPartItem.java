package com.titammods.hephaestus_tools.tools.part;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

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
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        MaterialId mat = getMaterial(stack);
        if (mat.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.hephaestus_tools.part.no_material")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.hephaestus_tools.material",
                Component.translatable("material.hephaestus_tools." + mat.id().getPath()))
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        var stats = com.titammods.hephaestus_tools.materials.MaterialManager
                .getInstance().getStatsForSlot(mat, partSlot);
        if (stats == null) return;

        if (partSlot == 0) {
            tooltip.add(stat("durability", String.valueOf(stats.durability())));
            tooltip.add(stat("mining_speed", fmt(stats.miningSpeed())));
            tooltip.add(stat("attack_damage", fmt(stats.attackDamage())));
            tooltip.add(stat("harvest_tier",
                    Component.translatable("tooltip.hephaestus_tools.tier."
                            + stats.tier().name().toLowerCase(java.util.Locale.ROOT)).getString()));
        } else if (partSlot == 1) {
            tooltip.add(stat("durability", pct(stats.durabilityMult())));
            tooltip.add(stat("mining_speed", pct(stats.speedMult())));
            tooltip.add(stat("attack_damage", pct(stats.damageMult())));
            tooltip.add(stat("attack_speed", pct(stats.attackSpeedMult())));
        }
    }

    private static Component stat(String key, String value) {
        return Component.translatable("tooltip.hephaestus_tools." + key, value)
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
    }

    private static String fmt(float f) {
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }

    private static String pct(float mult) {
        int p = Math.round(mult * 100f);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    @Override
    public Component getName(ItemStack stack) {
        MaterialId mat = getMaterial(stack);
        if (!mat.isEmpty()) {
            return Component.translatable(getDescriptionId())
                    .append(" (")
                    .append(Component.translatable("material.hephaestus_tools." + mat.id().getPath()))
                    .append(")");
        }
        return super.getName(stack);
    }
}
