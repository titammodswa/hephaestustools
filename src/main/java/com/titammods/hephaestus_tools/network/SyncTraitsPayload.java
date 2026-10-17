package com.titammods.hephaestus_tools.network;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.trait.TraitDefinition;
import com.titammods.hephaestus_tools.materials.trait.TraitManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SyncTraitsPayload(List<Entry> traits) implements CustomPacketPayload {

    public record Entry(Identifier id, TraitDefinition definition) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::id,
                ByteBufCodecs.fromCodecWithRegistries(TraitDefinition.CODEC), Entry::definition,
                Entry::new
        );
    }

    public static final Type<SyncTraitsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "sync_traits")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTraitsPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncTraitsPayload::traits,
            SyncTraitsPayload::new
    );

    public static SyncTraitsPayload of(Map<Identifier, TraitDefinition> traits) {
        List<Entry> entries = new ArrayList<>();
        traits.forEach((id, definition) -> entries.add(new Entry(id, definition)));
        return new SyncTraitsPayload(List.copyOf(entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTraitsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> TraitManager.receiveSync(payload.traits()));
    }
}
