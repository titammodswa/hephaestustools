package com.titammods.hephaestus_tools.network;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.tables.menu.WorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CraftAnimPayload(BlockPos pos, boolean tinker) implements CustomPacketPayload {

    public static final Type<CraftAnimPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "craft_anim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftAnimPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CraftAnimPayload::pos,
            ByteBufCodecs.BOOL, CraftAnimPayload::tinker,
            CraftAnimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CraftAnimPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof WorkbenchMenu menu)) return;
            if (!menu.getBlockEntity().getBlockPos().equals(payload.pos())) return;
            ItemStack shown = payload.tinker() ? menu.tool().copy() : menu.preview().copy();
            PacketDistributor.sendToPlayersTrackingEntity(player,
                    new RemoteCraftPayload(player.getId(), payload.pos(), payload.tinker(), shown));
        });
    }
}
