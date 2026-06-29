package com.titammods.hephaestus_tools.tools.helper;

import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.MaterialStats;
import com.titammods.hephaestus_tools.tools.item.ModifiableSwordItem;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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
        List<Component> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;

        if (ToolStack.isBroken(stack)) {
            out.add(prefix("durability").withStyle(ChatFormatting.GRAY).append(
                    Component.translatable("tooltip.hephaestus_tools.broken")
                            .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED)));
        } else {
            out.add(durabilityLine(stack));
        }

        out.addAll(modifierNames(stack));

        out.add(Component.empty());
        out.add(HOLD_SHIFT);
        out.add(HOLD_CTRL);
        return out;
    }

    public static List<Component> stats(ItemStack stack, boolean detailed) {
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
            if (p.getStat(ToolStats.FORTUNE, 0f) > 0f)
                out.add(statLine("fortune",
                        Component.literal("+" + (int) p.getStat(ToolStats.FORTUNE, 0f)), 0xFFFFFF));
            if (p.getStat(ToolStats.LOOTING, 0f) > 0f)
                out.add(statLine("looting",
                        Component.literal("+" + (int) p.getStat(ToolStats.LOOTING, 0f)), 0xFFFFFF));
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
                        out.add(statLine("durability", pct(st.durabilityMult()), 0xFFFFFF));
                        out.add(statLine("mining_speed", pct(st.speedMult()), 0xFFFFFF));
                        out.add(statLine("attack_damage", pct(st.damageMult()), 0xFFFFFF));
                        out.add(statLine("attack_speed", pct(st.attackSpeedMult()), 0xFFFFFF));
                    }
                    default -> {
                        if (st.durability() > 0)
                            out.add(statLine("durability", num(st.durability()), 0xFFFFFF));
                    }
                }
            }
            if (i != count - 1) out.add(Component.empty());
        }
        return out;
    }

    public static List<Component> modifiers(ItemStack stack, boolean detailed) {
        List<Component> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;

        for (ToolConstructionData.ModifierEntry mod : ToolStack.getModifiers(stack)) {
            ResourceLocation id = mod.id();
            String base = "modifier." + id.getNamespace() + "." + id.getPath();

            MutableComponent line = Component.translatable(base).withStyle(ChatFormatting.GRAY);
            if (mod.level() > 1) {
                line.append(Component.literal(" ")).append(roman(mod.level()));
            }
            out.add(line);

            if (detailed) {
                out.add(Component.literal("  ")
                        .append(Component.translatable(base + ".desc").withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        return out;
    }

    private static List<Component> modifierNames(ItemStack stack) {
        return modifiers(stack, false);
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

    private static MutableComponent pct(float mult) {
        int v = Math.round(mult * 100f);
        return Component.literal((v >= 0 ? "+" : "") + v + "%");
    }

    private static Component tierName(Tiers tier) {
        String name = switch (tier) {
            case WOOD -> "wood";
            case STONE -> "stone";
            case IRON -> "iron";
            case GOLD -> "gold";
            case DIAMOND -> "diamond";
            case NETHERITE -> "netherite";
        };
        return Component.translatable("tooltip.hephaestus_tools.tier." + name).withStyle(ChatFormatting.WHITE);
    }

    private static int valueToColor(float value, float max) {
        float ratio = max <= 0 ? 0f : value / max;
        float hue = Math.max(0.01f, Math.min(0.5f, ratio / 3f));
        return java.awt.Color.HSBtoRGB(hue, 0.65f, 0.8f) & 0xFFFFFF;
    }

    private static Component roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return Component.literal(n >= 0 && n < r.length ? r[n] : Integer.toString(n));
    }
}