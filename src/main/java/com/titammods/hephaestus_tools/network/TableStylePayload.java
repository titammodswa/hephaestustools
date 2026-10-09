package com.titammods.hephaestus_tools.network;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.table.TableStyle;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TableStylePayload(boolean modern) implements CustomPacketPayload {

    public static final Type<TableStylePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "table_style"));

    public static final StreamCodec<ByteBuf, TableStylePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TableStylePayload::modern,
            TableStylePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TableStylePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) TableStyle.set(player, payload.modern());
        });
    }
}
