package com.titammods.hephaestus_tools.client;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.config.HephaestusConfig;
import com.titammods.hephaestus_tools.network.TableStylePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, value = Dist.CLIENT)
public final class TableStyleClient {

    private TableStyleClient() {}

    public static void send() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) return;
        try {
            if (!connection.hasChannel(TableStylePayload.TYPE.id())) return;
            ClientPacketDistributor.sendToServer(new TableStylePayload(HephaestusConfig.modernInterface()));
        } catch (RuntimeException ignored) {
        }
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        send();
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == HephaestusConfig.SPEC) sendLater();
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == HephaestusConfig.SPEC) sendLater();
    }

    private static void sendLater() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(TableStyleClient::send);
    }
}