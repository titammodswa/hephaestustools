package com.titammods.hephaestus_tools.api.event;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge event bus each time a Hephaestus tool rebuilds its stats from its parts and
 * upgrades.
 * <p>
 * It fires once every value derived from the build has been written to the stack: the cached tool
 * stats, the current damage, the attack attribute modifiers, the mining rules, the maximum damage and
 * the enchantability. Listeners can read those finished values, or add and adjust components of their
 * own on the stack, for example to attach effects that belong to an addon's traits. Whatever a
 * listener sets stays on the stack until the next rebuild.
 * <p>
 * A rebuild happens when a tool is assembled, when a part is swapped or an upgrade is applied, when the
 * cached stats are missing, and when the loaded materials no longer match the ones the stats were built
 * from. It runs on both the logical client and the logical server, and also for preview stacks such as
 * creative tab entries, recipe results and the workbench preview. It never fires for a tool that has no
 * parts yet.
 * <p>
 * Rebuilding the same stack again from inside a listener posts this event again.
 */
public class ToolRecalculatedEvent extends Event {

    private final ItemStack stack;

    public ToolRecalculatedEvent(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }
}
