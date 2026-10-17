package com.titammods.hephaestus_tools.network;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.tier.TierDefinition;
import com.titammods.hephaestus_tools.materials.tier.TierManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record SyncTiersPayload(List<Entry> tiers) implements CustomPacketPayload {

    public record Entry(Identifier id, TierDefinition definition) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::id,
                TierDefinition.STREAM_CODEC, Entry::definition,
                Entry::new
        );
    }

    public static final Type<SyncTiersPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "sync_tiers")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTiersPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncTiersPayload::tiers,
            SyncTiersPayload::new
    );

    public static SyncTiersPayload of(Map<Identifier, TierDefinition> tiers) {
        List<Entry> entries = new ArrayList<>();
        tiers.forEach((id, definition) -> entries.add(new Entry(id, definition)));
        return new SyncTiersPayload(List.copyOf(entries));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTiersPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> TierManager.receiveSync(payload.tiers()));
    }
}
