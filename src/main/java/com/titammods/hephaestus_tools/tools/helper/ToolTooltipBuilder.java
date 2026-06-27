package com.titammods.hephaestus_tools.tools.helper;

import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.stat.ToolStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ToolTooltipBuilder {

    private ToolTooltipBuilder() {}

    public static List<Component> stats(ItemStack stack, boolean detailed) {
        List<Component> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;

        ToolPropertiesData p = ToolStack.getProperties(stack);

        int max = p.getDurability();
        int cur = Math.max(0, max - ToolStack.getCurrentDamage(stack));
        out.add(statLine("durability",
                Component.literal(cur + " / " + max).withStyle(durabilityColor(cur, max))));

        out.add(statLine("harvest_tier", tierName(p.getHarvestTier())));
        out.add(statLine("mining_speed", val(p.getMiningSpeed())));
        out.add(statLine("attack_damage", val(p.getAttackDamage())));
        out.add(statLine("attack_speed", val(p.getAttackSpeed())));

        if (detailed) {
            out.add(statLine("enchantability",
                    Component.literal(Integer.toString(p.getEnchantability())).withStyle(ChatFormatting.WHITE)));
            if (p.getStat(ToolStats.FORTUNE, 0f) > 0f)
                out.add(statLine("fortune",
                        Component.literal("+" + (int) p.getStat(ToolStats.FORTUNE, 0f)).withStyle(ChatFormatting.WHITE)));
            if (p.getStat(ToolStats.LOOTING, 0f) > 0f)
                out.add(statLine("looting",
                        Component.literal("+" + (int) p.getStat(ToolStats.LOOTING, 0f)).withStyle(ChatFormatting.WHITE)));
            if (p.getStat(ToolStats.INDESTRUCTIBLE, 0f) > 0f)
                out.add(Component.translatable("tooltip.hephaestus_tools.indestructible")
                        .withStyle(ChatFormatting.AQUA));
        }
        return out;
    }

    public static List<Component> modifiers(ItemStack stack, boolean detailed) {
        List<Component> out = new ArrayList<>();
        if (!ToolStack.isInitialized(stack)) return out;

        for (ToolConstructionData.ModifierEntry mod : ToolStack.getModifiers(stack)) {
            ResourceLocation id = mod.id();
            String base = "modifier." + id.getNamespace() + "." + id.getPath();

            MutableComponent line = Component.translatable(base).withStyle(ChatFormatting.LIGHT_PURPLE);
            if (mod.level() > 1) {
                line.append(Component.literal(" ")).append(roman(mod.level()));
            }
            out.add(line);

            if (detailed) {
                out.add(Component.literal("  ")
                        .append(Component.translatable(base + ".desc").withStyle(ChatFormatting.GRAY)));
            }
        }
        return out;
    }

    private static Component statLine(String key, Component value) {
        return Component.translatable("tooltip.hephaestus_tools." + key, value)
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component val(float f) {
        return Component.literal(String.format(Locale.ROOT, "%.2f", f)).withStyle(ChatFormatting.WHITE);
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

    private static ChatFormatting durabilityColor(int cur, int max) {
        float r = max <= 0 ? 0f : (float) cur / max;
        if (r > 0.5f) return ChatFormatting.GREEN;
        if (r > 0.25f) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static Component roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return Component.literal(n >= 0 && n < r.length ? r[n] : Integer.toString(n));
    }
}
