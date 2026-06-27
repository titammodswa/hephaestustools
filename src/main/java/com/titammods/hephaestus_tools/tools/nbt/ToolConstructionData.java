package com.titammods.hephaestus_tools.tools.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.hephaestus_tools.materials.MaterialId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ToolConstructionData(
        List<MaterialId> materials,
        List<ModifierEntry> modifiers,
        int damage,
        boolean broken
) {
    public static final ToolConstructionData EMPTY = new ToolConstructionData(
            List.of(), List.of(), 0, false
    );

    public static final Codec<ToolConstructionData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MaterialId.CODEC.listOf().fieldOf("materials").forGetter(ToolConstructionData::materials),
                    ModifierEntry.CODEC.listOf().fieldOf("modifiers").forGetter(ToolConstructionData::modifiers),
                    Codec.INT.optionalFieldOf("damage", 0).forGetter(ToolConstructionData::damage),
                    Codec.BOOL.optionalFieldOf("broken", false).forGetter(ToolConstructionData::broken)
            ).apply(instance, ToolConstructionData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolConstructionData> STREAM_CODEC =
            StreamCodec.composite(
                    MaterialId.STREAM_CODEC.apply(ByteBufCodecs.list()), ToolConstructionData::materials,
                    ModifierEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), ToolConstructionData::modifiers,
                    ByteBufCodecs.VAR_INT, ToolConstructionData::damage,
                    ByteBufCodecs.BOOL, ToolConstructionData::broken,
                    ToolConstructionData::new
            );

    public ToolConstructionData withDamage(int newDamage) {
        return new ToolConstructionData(materials, modifiers, newDamage, broken);
    }

    public ToolConstructionData withBroken(boolean isBroken) {
        return new ToolConstructionData(materials, modifiers, damage, isBroken);
    }

    public boolean hasMaterial(int index) {
        return index >= 0 && index < materials.size() && !materials.get(index).isEmpty();
    }

    public MaterialId getMaterial(int index) {
        if (index < 0 || index >= materials.size()) return MaterialId.EMPTY;
        return materials.get(index);
    }

    public boolean isInitialized() {
        return !materials.isEmpty() && materials.stream().anyMatch(m -> !m.isEmpty());
    }

    public record ModifierEntry(ResourceLocation id, int level) {
        public static final Codec<ModifierEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(ModifierEntry::id),
                        Codec.INT.fieldOf("level").forGetter(ModifierEntry::level)
                ).apply(instance, ModifierEntry::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ModifierEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, ModifierEntry::id,
                        ByteBufCodecs.VAR_INT, ModifierEntry::level,
                        ModifierEntry::new
                );
    }
}
