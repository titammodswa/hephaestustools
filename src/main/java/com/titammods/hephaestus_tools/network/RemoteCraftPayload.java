package com.titammods.hephaestus_tools.network;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RemoteCraftPayload(int entityId, BlockPos pos, boolean tinker, ItemStack subject) implements CustomPacketPayload {

    public static final Type<RemoteCraftPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "remote_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteCraftPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemoteCraftPayload::entityId,
            BlockPos.STREAM_CODEC, RemoteCraftPayload::pos,
            ByteBufCodecs.BOOL, RemoteCraftPayload::tinker,
            ItemStack.OPTIONAL_STREAM_CODEC, RemoteCraftPayload::subject,
            RemoteCraftPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoteCraftPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.titammods.hephaestus_tools.client.workbench.RemoteCraft.start(
                payload.entityId(), payload.pos(), payload.tinker(), payload.subject()));
    }
}
