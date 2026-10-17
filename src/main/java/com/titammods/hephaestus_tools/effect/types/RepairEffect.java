package com.titammods.hephaestus_tools.effect.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.LevelValue;
import com.titammods.hephaestus_tools.effect.ToolEffect;
import net.minecraft.world.item.ItemStack;

public record RepairEffect(int interval, LevelValue amount, boolean whileBroken) implements ToolEffect {

    public static final MapCodec<RepairEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("interval").forGetter(RepairEffect::interval),
                    LevelValue.CODEC.optionalFieldOf("amount", LevelValue.of(1f)).forGetter(RepairEffect::amount),
                    Codec.BOOL.optionalFieldOf("while_broken", false).forGetter(RepairEffect::whileBroken)
            ).apply(instance, RepairEffect::new));

    @Override
    public MapCodec<RepairEffect> codec() {
        return CODEC;
    }

    @Override
    public boolean ticksWhileBroken() {
        return whileBroken;
    }

    @Override
    public void inventoryTick(EffectContext context, int level) {
        ItemStack tool = context.tool();
        if (context.level().getGameTime() % interval == 0L && tool.isDamaged()) {
            tool.setDamageValue(tool.getDamageValue() - (int) amount.get(level));
        }
    }
}
