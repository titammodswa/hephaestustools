package com.titammods.hephaestus_tools.table;

import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public final class TableStyle {

    private static final Map<UUID, Boolean> MODERN = new ConcurrentHashMap<>();

    private TableStyle() {}

    public static boolean isModern(Player player) {
        if (player == null) return true;
        return MODERN.getOrDefault(player.getUUID(), Boolean.TRUE);
    }

    public static void set(Player player, boolean modern) {
        if (player != null) MODERN.put(player.getUUID(), modern);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MODERN.remove(event.getEntity().getUUID());
    }
}
