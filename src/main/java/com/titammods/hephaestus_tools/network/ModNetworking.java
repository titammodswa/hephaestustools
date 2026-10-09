package com.titammods.hephaestus_tools.network;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public class ModNetworking {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(HephaestusTools.MOD_ID).versioned("1.0.0");
        reg.playToClient(
                SyncMaterialsPayload.TYPE,
                SyncMaterialsPayload.STREAM_CODEC,
                SyncMaterialsPayload::handle
        );
        reg.playToServer(TableStylePayload.TYPE, TableStylePayload.STREAM_CODEC, TableStylePayload::handle);
        reg.playToServer(CraftAnimPayload.TYPE, CraftAnimPayload.STREAM_CODEC, CraftAnimPayload::handle);
        reg.playToClient(RemoteCraftPayload.TYPE, RemoteCraftPayload.STREAM_CODEC, RemoteCraftPayload::handle);
    }
}
