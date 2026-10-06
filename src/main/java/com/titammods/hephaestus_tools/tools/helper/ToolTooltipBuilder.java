package com.titammods.hephaestus_tools.tools.helper;

import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.MaterialStats;
import com.titammods.hephaestus_tools.tools.item.ModifiableSwordItem;
import com.titammods.hephaestus_tools.tools.modifier.ModifierEffects;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import com.titammods.hephaestus_tools.tools.stat.HarvestTier;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolTooltipBuilder {

    private ToolTooltipBuilder() {}

    private static final int C_DURABILITY    = 0x47CC47;
    private static final int C_ATTACK_DAMAGE = 0xD76464;
    private static final int C_ATTACK_SPEED  = 0x8547CC;
    private static final int C_MINING_SPEED  = 0x78A0CD;
    private static final int C_DURABILITY_MAX = valueToColor(1f, 1f);

    private static final DecimalFormat COMMA   = new DecimalFormat("#,##0");
    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0.##");

    private static final Component HOLD_SHIFT = Component.translatable(
            "tooltip.hephaestus_tools.hold_shift",
            Component.translatable("key.hephaestus_tools.shift").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    private static final Component HOLD_CTRL = Component.translatable(
            "tooltip.hephaestus_tools.hold_ctrl",
            Component.translatable("key.hephaestus_tools.ctrl").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));

    public static List<Component> defaultInfo(ItemStack stack) {
        return defaultInfo(stack, Map.of());
    }

    public static List<Component> defaultInfo(ItemStack stack, Map<Identifier, Integer> bonus) {
        List<Component> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;

        if (ToolStack.isBroken(stack)) {
            out.add(prefix("durability").withStyle(ChatFormatting.GRAY).append(
                    Component.translatable("tooltip.hephaestus_tools.broken")
                            .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED)));
        } else {
            out.add(durabilityLine(stack));
        }

        out.addAll(modifiers(stack, false, bonus));

        out.add(Component.empty());
        out.add(HOLD_SHIFT);
        out.add(HOLD_CTRL);
        return out;
    }

    public static List<Component> stats(ItemStack stack, boolean detailed) {
        return stats(stack, detailed, Map.of());
    }

    public static List<Component> stats(ItemStack stack, boolean detailed, Map<Identifier, Integer> bonus) {
        List<Component> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;

        ToolPropertiesData p = ToolStack.getProperties(stack);
        boolean weapon = stack.getItem() instanceof ModifiableSwordItem;

        out.add(durabilityLine(stack));

        out.add(statLine("attack_damage", num(1.0f + p.getAttackDamage()), C_ATTACK_DAMAGE));
        out.add(statLine("attack_speed", num(p.getAttackSpeed()), C_ATTACK_SPEED));

        if (!weapon) {
            out.add(prefixLine("harvest_tier", tierName(p.getHarvestTier())));
            out.add(statLine("mining_speed", num(p.getMiningSpeed()), C_MINING_SPEED));
        }

        if (detailed) {
            out.add(statLine("enchantability",
                    Component.literal(Integer.toString(p.getEnchantability())), 0xFFFFFF));
            int fortune = (int) p.getStat(ToolStats.FORTUNE, 0f) + bonus.getOrDefault(ModifierEffects.FORTUNE, 0);
            if (fortune > 0)
                out.add(statLine("fortune", Component.literal("+" + fortune), 0xFFFFFF));
            int looting = (int) p.getStat(ToolStats.LOOTING, 0f) + bonus.getOrDefault(ModifierEffects.LOOTING, 0);
            if (looting > 0)
                out.add(statLine("looting", Component.literal("+" + looting), 0xFFFFFF));
            if (p.getStat(ToolStats.INDESTRUCTIBLE, 0f) > 0f)
                out.add(Component.translatable("tooltip.hephaestus_tools.indestructible")
                        .withStyle(ChatFormatting.AQUA));
        }
        return out;
    }

    public static List<Component> components(ItemStack stack, Item toolItem) {
        List<Component> out = new ArrayList<>();
        List<MaterialId> materials = ToolStack.getMaterials(stack);
        List<Item> parts = ToolBuildHandler.getToolParts(toolItem);
        if (materials.isEmpty() || parts.isEmpty()) {
            out.add(Component.translatable("tooltip.hephaestus_tools.no_material").withStyle(ChatFormatting.GRAY));
            return out;
        }

        MaterialManager mm = MaterialManager.getInstance();
        int count = Math.min(materials.size(), parts.size());
        for (int i = 0; i < count; i++) {
            MaterialId matId = materials.get(i);
            Item partItem = parts.get(i);
            if (!(partItem instanceof ToolPartItem part)) continue;

            Material material = mm.getMaterial(matId);
            int color = (material != null) ? (material.color() & 0xFFFFFF) : 0xFFFFFF;

            Component name = part.withMaterial(matId).getHoverName().copy()
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(s -> s.withColor(color));
            out.add(name);

            MaterialStats st = mm.getStatsForSlot(matId, part.getPartSlot());
            if (st != null) {
                switch (part.getPartSlot()) {
                    case 0 -> {
                        out.add(statLine("durability", num(st.durability()), C_DURABILITY));
                        out.add(statLine("attack_damage", num(st.attackDamage()), C_ATTACK_DAMAGE));
                        out.add(statLine("mining_speed", num(st.miningSpeed()), C_MINING_SPEED));
                        out.add(prefixLine("harvest_tier", tierName(st.tier())));
                    }
                    case 1 -> {
                        ToolBuildHandler.SupportStats sup = ToolBuildHandler.supportStats(matId, 1);
                        out.add(statLine("durability",
                                plus(sup.durability()).append(Component.literal(" "))
                                        .append(pct(st.durabilityMult())), C_DURABILITY));
                        out.add(statLine("mining_speed",
                                plus(sup.miningSpeed()).append(Component.literal(" "))
                                        .append(pct(st.speedMult())), C_MINING_SPEED));
                        out.add(statLine("attack_damage",
                                plus(sup.attackDamage()).append(Component.literal(" "))
                                        .append(pct(st.damageMult())), C_ATTACK_DAMAGE));
                        out.add(statLine("attack_speed", pct(st.attackSpeedMult()), C_ATTACK_SPEED));
                        out.add(statLine("enchantability", plus(sup.enchantability()), 0xFFFFFF));
                    }
                    default -> {
                        ToolBuildHandler.SupportStats sup = ToolBuildHandler.supportStats(matId, 2);
                        out.add(statLine("durability", plus(sup.durability()), C_DURABILITY));
                        out.add(statLine("mining_speed", plus(sup.miningSpeed()), C_MINING_SPEED));
                        out.add(statLine("attack_damage", plus(sup.attackDamage()), C_ATTACK_DAMAGE));
                        out.add(statLine("enchantability", plus(sup.enchantability()), 0xFFFFFF));
                    }
                }
            }
            if (i != count - 1) out.add(Component.empty());
        }
        return out;
    }

    public static List<Component> modifiers(ItemStack stack, boolean detailed) {
        return modifiers(stack, detailed, Map.of());
    }

    public static List<Component> modifiers(ItemStack stack, boolean detailed, Map<Identifier, Integer> bonus) {
        List<Component> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;

        Map<Identifier, Integer> remaining = new LinkedHashMap<>(bonus);
        for (ToolConstructionData.ModifierEntry mod : ToolStack.getModifiers(stack)) {
            Integer extra = remaining.remove(mod.id());
            addModifier(out, mod.id(), mod.level() + (extra == null ? 0 : extra), detailed);
        }
        remaining.forEach((id, extra) -> addModifier(out, id, extra, detailed));
        return out;
    }

    private static void addModifier(List<Component> out, Identifier id, int level, boolean detailed) {
        String base = "modifier." + id.getNamespace() + "." + id.getPath();

        MutableComponent line = Component.translatable(base).withStyle(ChatFormatting.GRAY);
        if (level > 1) {
            line.append(Component.literal(" ")).append(roman(level));
        }
        out.add(line);

        if (detailed) {
            out.add(Component.literal("  ")
                    .append(Component.translatable(base + ".desc").withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    private static MutableComponent prefix(String statKey) {
        return Component.translatable("tool_stat.hephaestus_tools." + statKey);
    }

    private static Component statLine(String statKey, MutableComponent value, int rgb) {
        return prefix(statKey).withStyle(ChatFormatting.GRAY)
                .append(value.withStyle(s -> s.withColor(rgb)));
    }

    private static Component prefixLine(String statKey, Component value) {
        return prefix(statKey).withStyle(ChatFormatting.GRAY).append(value);
    }

    private static Component durabilityLine(ItemStack stack) {
        ToolPropertiesData p = ToolStack.getProperties(stack);
        int max = p.getDurability();
        int cur = Math.max(0, max - ToolStack.getCurrentDamage(stack));
        return prefix("durability").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(COMMA.format(cur)).withStyle(s -> s.withColor(valueToColor(cur, max))))
                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(COMMA.format(max)).withStyle(s -> s.withColor(C_DURABILITY_MAX)));
    }

    private static MutableComponent num(float f) {
        return Component.literal(DECIMAL.format(f));
    }

    private static MutableComponent plus(float value) {
        return Component.literal((value >= 0f ? "+" : "") + DECIMAL.format(value));
    }

    private static MutableComponent pct(float mult) {
        int v = Math.round(mult * 100f);
        return Component.literal((v >= 0 ? "+" : "") + v + "%");
    }

    private static Component tierName(HarvestTier tier) {
        return Component.translatable("tooltip.hephaestus_tools.tier." + tier.id()).withStyle(ChatFormatting.WHITE);
    }

    private static int valueToColor(float value, float max) {
        float ratio = max <= 0 ? 0f : value / max;
        float hue = Math.max(0.01f, Math.min(0.5f, ratio / 3f));
        return java.awt.Color.HSBtoRGB(hue, 0.65f, 0.8f) & 0xFFFFFF;
    }

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private static Component roman(int n) {
        if (n <= 0 || n >= 4000) return Component.literal(Integer.toString(n));
        StringBuilder numeral = new StringBuilder();
        int rest = n;
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (rest >= ROMAN_VALUES[i]) {
                numeral.append(ROMAN_SYMBOLS[i]);
                rest -= ROMAN_VALUES[i];
            }
        }
        return Component.literal(numeral.toString());
    }
}