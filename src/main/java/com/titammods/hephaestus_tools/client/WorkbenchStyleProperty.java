package com.titammods.hephaestus_tools.client;

import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.config.HephaestusConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record WorkbenchStyleProperty() implements ConditionalItemModelProperty {

    public static final MapCodec<WorkbenchStyleProperty> MAP_CODEC = MapCodec.unit(new WorkbenchStyleProperty());

    @Override
    public boolean get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        return HephaestusConfig.modernInterface();
    }

    @Override
    public MapCodec<WorkbenchStyleProperty> type() {
        return MAP_CODEC;
    }
}
