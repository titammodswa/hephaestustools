package com.titammods.hephaestus_tools.api.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.function.Consumer;

/**
 * Posted on the NeoForge event bus while a Hephaestus tool's tooltip is built, right after the lines
 * that list the traits of the tool's materials.
 * <p>
 * Lines added here appear directly below Tools' own trait lines and above the tool's level and
 * experience lines, so an addon can list its own traits in the same place as the built-in ones. Lines
 * appear in the order listeners add them. The event fires even when the tool has no traits, at the spot
 * the trait lines would take.
 * <p>
 * It fires wherever the tooltip is built, which is usually the client, and never for a tool that has no
 * parts yet.
 */
public class ToolTraitTooltipEvent extends Event {

    private final ItemStack stack;
    private final Consumer<Component> tooltip;

    public ToolTraitTooltipEvent(ItemStack stack, Consumer<Component> tooltip) {
        this.stack = stack;
        this.tooltip = tooltip;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Consumer<Component> getTooltip() {
        return tooltip;
    }

    public void add(Component line) {
        tooltip.accept(line);
    }
}
