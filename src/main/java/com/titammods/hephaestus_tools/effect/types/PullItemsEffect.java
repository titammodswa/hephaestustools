package com.titammods.hephaestus_tools.effect.types;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.effect.EffectContext;
import com.titammods.hephaestus_tools.effect.ToolEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record PullItemsEffect(int interval, double radius, double strength) implements ToolEffect {

    public static final MapCodec<PullItemsEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("interval").forGetter(PullItemsEffect::interval),
                    Codec.DOUBLE.fieldOf("radius").forGetter(PullItemsEffect::radius),
                    Codec.DOUBLE.fieldOf("strength").forGetter(PullItemsEffect::strength)
            ).apply(instance, PullItemsEffect::new));

    @Override
    public MapCodec<PullItemsEffect> codec() {
        return CODEC;
    }

    @Override
    public void inventoryTick(EffectContext context, int level) {
        Level world = context.level();
        if (world.isClientSide() || world.getGameTime() % interval != 0L) return;
        LivingEntity holder = context.holder();
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, holder.getBoundingBox().inflate(radius))) {
            if (item.hasPickUpDelay() || !item.isAlive()) continue;
            Vec3 pull = holder.position().add(0, 0.5, 0).subtract(item.position()).normalize().scale(strength);
            item.setDeltaMovement(item.getDeltaMovement().add(pull));
        }
    }
}
