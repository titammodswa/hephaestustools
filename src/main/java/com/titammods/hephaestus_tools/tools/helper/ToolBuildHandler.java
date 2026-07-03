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

        MaterialManager mm = MaterialManager.getInstance();
        List<net.minecraft.world.item.Item> layoutParts = getToolParts(stack.getItem());

        List<MaterialStats> headParts = new ArrayList<>();
        List<MaterialStats> handleParts = new ArrayList<>();
        List<MaterialStats> bindingParts = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            int role;
            if (i < layoutParts.size()
                    && layoutParts.get(i) instanceof com.titammods.hephaestus_tools.tools.part.ToolPartItem tp) {
                role = tp.getPartSlot();
            } else {
                role = (i == 0) ? 0 : (i == 1) ? 1 : (i == 2) ? 2 : 1;
            }
            MaterialId id = materials.get(i);
            switch (role) {
                case 0 -> headParts.add(mm.getStatsForSlot(id, 0));
                case 2 -> bindingParts.add(mm.getStatsForSlot(id, 2));
                default -> handleParts.add(mm.getStatsForSlot(id, 1));
            }
        }
        if (headParts.isEmpty()) headParts.add(mm.getStats(materials.get(0)));

        float baseDurability = 0, baseMiningSpeed = 0, baseAttackDamage = 0, baseAttackSpeed = 0;
        int enchAccum = 0;
        for (MaterialStats hs : headParts) {
            baseDurability += hs.durability();
            baseMiningSpeed += hs.miningSpeed();
            baseAttackDamage += hs.attackDamage();
            baseAttackSpeed += hs.attackSpeed();
            enchAccum += hs.enchantability();
        }
        int hc = headParts.size();
        baseDurability /= hc;
        baseMiningSpeed /= hc;
        baseAttackDamage /= hc;
        baseAttackSpeed /= hc;
        Tiers harvestTier = headParts.get(0).tier();
        int baseEnchantability = Math.round(enchAccum / (float) hc);

        float durabilityMult = 1.0f;
        float speedMult = 1.0f;
        float damageMult = 1.0f;
        float attackSpeedMult = 1.0f;
        for (int h = 0; h < handleParts.size(); h++) {
            MaterialStats hs = handleParts.get(h);
            float w = (h == 0) ? 1.0f : 0.5f;
            durabilityMult += hs.durabilityMult() * w;
            speedMult += hs.speedMult() * w;
            damageMult += hs.damageMult() * w;
            attackSpeedMult += hs.attackSpeedMult() * w;
        }

        float bindingDurabilityBonus = 0f;
        for (MaterialStats bs : bindingParts) {
            bindingDurabilityBonus += bs.durability();
        }

        int finalDurability = Math.max(1, (int)((baseDurability + bindingDurabilityBonus) * durabilityMult));
        float finalMiningSpeed = Math.max(0.1f, baseMiningSpeed * speedMult);
        float finalAttackDamage = Math.max(0f, baseAttackDamage * damageMult);
        float finalAttackSpeed = Math.max(0f, baseAttackSpeed * attackSpeedMult);

        if (stack.getItem() instanceof com.titammods.hephaestus_tools.tools.item.ModifiableItem mi) {
            finalAttackDamage = Math.max(0f,
                    (baseAttackDamage + mi.getAttackDamageBonus()) * damageMult * mi.getAttackDamageMultiplier());
            float speedOverride = mi.getBaseAttackSpeed();
            if (speedOverride >= 0f) finalAttackSpeed = speedOverride;
        }

        ModifierStatContext ctx = new ModifierStatContext(
                finalDurability, finalMiningSpeed, finalAttackDamage,
                finalAttackSpeed, baseEnchantability, harvestTier);

        if (!construction.materials().isEmpty()) {
            var headMat = construction.materials().get(0);
            var headStats = com.titammods.hephaestus_tools.materials.MaterialManager
                    .getInstance().getStatsForSlot(headMat, 0);
            if (headStats != null && headStats.hasTrait()) {
                com.titammods.hephaestus_tools.materials.trait.MaterialTrait
                        .byId(headStats.traitId()).ifPresent(t -> t.applyStats(ctx));
            }
        }

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