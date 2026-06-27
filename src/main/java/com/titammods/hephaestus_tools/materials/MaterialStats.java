package com.titammods.hephaestus_tools.materials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Tiers;

public record MaterialStats(
        int durability,
        float miningSpeed,
        float attackDamage,
        float attackSpeed,
        int tierOrdinal,
        int enchantability,
        float durabilityMult,
        float speedMult,
        float damageMult,
        float attackSpeedMult,
        String traitId
) {
    public static final MaterialStats EMPTY = new MaterialStats(
            1, 1f, 1f, 1f, 0, 0,
            0f, 0f, 0f, 0f, ""
    );

    public static final Codec<MaterialStats> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("durability", 1).forGetter(MaterialStats::durability),
                    Codec.FLOAT.optionalFieldOf("mining_speed", 1f).forGetter(MaterialStats::miningSpeed),
                    Codec.FLOAT.optionalFieldOf("attack_damage", 1f).forGetter(MaterialStats::attackDamage),
                    Codec.FLOAT.optionalFieldOf("attack_speed", 1f).forGetter(MaterialStats::attackSpeed),
                    Codec.INT.optionalFieldOf("tier", 0).forGetter(MaterialStats::tierOrdinal),
                    Codec.INT.optionalFieldOf("enchantability", 0).forGetter(MaterialStats::enchantability),
                    Codec.FLOAT.optionalFieldOf("durability_mult", 0f).forGetter(MaterialStats::durabilityMult),
                    Codec.FLOAT.optionalFieldOf("speed_mult", 0f).forGetter(MaterialStats::speedMult),
                    Codec.FLOAT.optionalFieldOf("damage_mult", 0f).forGetter(MaterialStats::damageMult),
                    Codec.FLOAT.optionalFieldOf("attack_speed_mult", 0f).forGetter(MaterialStats::attackSpeedMult),
                    Codec.STRING.optionalFieldOf("trait", "").forGetter(MaterialStats::traitId)
            ).apply(instance, MaterialStats::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialStats> STREAM_CODEC =
            StreamCodec.of(
                    (buf, s) -> {
                        buf.writeVarInt(s.durability);
                        buf.writeFloat(s.miningSpeed);
                        buf.writeFloat(s.attackDamage);
                        buf.writeFloat(s.attackSpeed);
                        buf.writeVarInt(s.tierOrdinal);
                        buf.writeVarInt(s.enchantability);
                        buf.writeFloat(s.durabilityMult);
                        buf.writeFloat(s.speedMult);
                        buf.writeFloat(s.damageMult);
                        buf.writeFloat(s.attackSpeedMult);
                        buf.writeUtf(s.traitId != null ? s.traitId : "");
                    },
                    buf -> new MaterialStats(
                            buf.readVarInt(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readUtf()
                    )
            );

    public Tiers tier() {
        Tiers[] values = Tiers.values();
        if (tierOrdinal < 0 || tierOrdinal >= values.length) return Tiers.WOOD;
        return values[tierOrdinal];
    }

    public boolean hasTrait() {
        return traitId != null && !traitId.isEmpty();
    }
}