package com.titammods.hephaestus_tools.tables.layout;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.List;

public record ToolLayout(
        String id,
        Component displayName,
        Component description,
        Item iconItem,
        ToolSlotDef toolSlot,
        List<InputSlotDef> inputs
) {
    public record ToolSlotDef(int x, int y, boolean hidden, @Nullable ResourceLocation iconTexture) {
        public ToolSlotDef(int x, int y) { this(x, y, false, null); }
        public ToolSlotDef(int x, int y, ResourceLocation icon) { this(x, y, false, icon); }
    }

    public record InputSlotDef(int x, int y, @Nullable Item expectedPart,
                               @Nullable ResourceLocation iconTexture) {
        public InputSlotDef(int x, int y) { this(x, y, null, null); }
        public InputSlotDef(int x, int y, Item part) { this(x, y, part, null); }
    }

    public int inputCount() { return inputs.size(); }
}