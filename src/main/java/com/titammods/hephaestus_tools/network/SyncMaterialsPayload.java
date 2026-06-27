package com.titammods.hephaestus_tools.network;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.Material;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncMaterialsPayload(Map<MaterialId, Material> materials) implements CustomPacketPayload {

    public static final Type<SyncMaterialsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HephaestusTools.MOD_ID, "sync_materials")
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, HashMap<MaterialId, Material>> MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,
                    MaterialId.STREAM_CODEC,
                    Material.STREAM_CODEC
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMaterialsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, data) -> MAP_CODEC.encode(buf, new HashMap<>(data.materials)),
                    buf -> new SyncMaterialsPayload(MAP_CODEC.decode(buf))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncMaterialsPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> MaterialManager.receiveSync(payload.materials()));
    }
}
