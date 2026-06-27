package com.titammods.hephaestus_tools.tools.helper;

import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.MaterialStats;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import com.titammods.hephaestus_tools.tools.modifier.ModifierEffect;
import com.titammods.hephaestus_tools.tools.modifier.ModifierEffects;
import com.titammods.hephaestus_tools.tools.modifier.ModifierStatContext;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

import java.util.ArrayList;
import java.util.List;

public final class ToolBuildHandler {

    private ToolBuildHandler() {}

    public static ItemStack buildTool(net.minecraft.world.item.Item toolItem, List<MaterialId> materials) {
        ItemStack stack = new ItemStack(toolItem);
        com.titammods.hephaestus_tools.tools.nbt.ToolStack.setMaterials(stack, List.copyOf(materials));
        com.titammods.hephaestus_tools.tools.nbt.ToolStack.recalculate(stack);
        return stack;
    }

    public static ItemStack buildSingleMaterial(net.minecraft.world.item.Item toolItem, MaterialId materialId) {
        int partCount = getPartCount(toolItem);
        List<MaterialId> materials = new ArrayList<>(partCount);
        for (int i = 0; i < partCount; i++) {
            materials.add(materialId);
        }
        return buildTool(toolItem, materials);
    }

    public static int getPartCount(net.minecraft.world.item.Item toolItem) {
        for (var layout : com.titammods.hephaestus_tools.tables.layout.ToolLayouts.ALL) {
            if (layout.iconItem() == toolItem) {
                return Math.max(1, layout.inputs().size());
            }
        }
        return 3;
    }

    public static List<net.minecraft.world.item.Item> getToolParts(net.minecraft.world.item.Item toolItem) {
        for (var layout : com.titammods.hephaestus_tools.tables.layout.ToolLayouts.ALL) {
            if (layout.iconItem() == toolItem) {
                List<net.minecraft.world.item.Item> parts = new ArrayList<>();
                for (var def : layout.inputs()) {
                    if (def.expectedPart() != null) parts.add(def.expectedPart());
                }
                return parts;
            }
        }
        return List.of();
    }

    public static ToolPropertiesData calculateProperties(ItemStack stack, ToolConstructionData construction) {
        List<MaterialId> materials = construction.materials();

        if (materials.isEmpty()) return ToolPropertiesData.EMPTY;

        ToolPropertiesData.Builder builder = ToolPropertiesData.builder();

        MaterialId headId = materials.isEmpty() ? MaterialId.EMPTY : materials.get(0);
        MaterialStats headStats = MaterialManager.getInstance().getStats(headId);

        float baseDurability = headStats.durability();
        float baseMiningSpeed = headStats.miningSpeed();
        float baseAttackDamage = headStats.attackDamage();
        float baseAttackSpeed = headStats.attackSpeed();
        Tiers harvestTier = headStats.tier();
        int baseEnchantability = headStats.enchantability();

        float durabilityMult = 1.0f;
        float speedMult = 1.0f;
        float damageMult = 1.0f;
        float attackSpeedMult = 1.0f;

        if (materials.size() > 1) {
            MaterialId handleId = materials.get(1);
            MaterialStats handleStats = MaterialManager.getInstance().getStatsForSlot(handleId, 1);
            durabilityMult += handleStats.durabilityMult();
            speedMult += handleStats.speedMult();
            damageMult += handleStats.damageMult();
            attackSpeedMult += handleStats.attackSpeedMult();
        }

        float bindingDurabilityBonus = 0f;
        if (materials.size() > 2) {
            MaterialId bindingId = materials.get(2);
            MaterialStats bindingStats = MaterialManager.getInstance().getStatsForSlot(bindingId, 2);
            bindingDurabilityBonus = bindingStats.durability();
        }

        if (materials.size() > 3) {
            MaterialId toughHandleId = materials.get(3);
            MaterialStats toughHandleStats = MaterialManager.getInstance().getStatsForSlot(toughHandleId, 1);
            durabilityMult += toughHandleStats.durabilityMult() * 0.5f;
        }

        int finalDurability = Math.max(1, (int)((baseDurability + bindingDurabilityBonus) * durabilityMult));
        float finalMiningSpeed = Math.max(0.1f, baseMiningSpeed * speedMult);
        float finalAttackDamage = Math.max(0f, baseAttackDamage * damageMult);
        float finalAttackSpeed = Math.max(0f, baseAttackSpeed * attackSpeedMult);

        ModifierStatContext ctx = new ModifierStatContext(
                finalDurability, finalMiningSpeed, finalAttackDamage,
                finalAttackSpeed, baseEnchantability, harvestTier);

        List<String> activeTraits = new ArrayList<>();
        for (ToolConstructionData.ModifierEntry modEntry : construction.modifiers()) {
            activeTraits.add(modEntry.id().toString());
            ModifierEffect effect = ModifierEffects.get(modEntry.id());
            if (effect != null) {
                effect.apply(ctx, modEntry.level());
            }
        }

        builder.durability(Math.max(1, (int) ctx.durability))
                .miningSpeed(Math.max(0.1f, ctx.miningSpeed))
                .attackDamage(Math.max(0f, ctx.attackDamage))
                .attackSpeed(Math.max(0f, ctx.attackSpeed))
                .harvestTier(ctx.harvestTier)
                .enchantability(ctx.enchantability)
                .traits(activeTraits);

        if (ctx.fortune > 0)      builder.stat(ToolStats.FORTUNE, ctx.fortune);
        if (ctx.looting > 0)      builder.stat(ToolStats.LOOTING, ctx.looting);
        if (ctx.indestructible)   builder.stat(ToolStats.INDESTRUCTIBLE, 1f);

        return builder.build();
    }
}