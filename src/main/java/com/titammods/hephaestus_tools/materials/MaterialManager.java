package com.titammods.hephaestus_tools.materials;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.network.SyncMaterialsPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public class MaterialManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FOLDER = "hephaestus_tools/materials";

    private static MaterialManager INSTANCE = new MaterialManager();

    private final Map<MaterialId, Material> materials = new LinkedHashMap<>();

    private MaterialManager() {
        super(GSON, FOLDER);
    }

    public static MaterialManager getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        materials.clear();
        int loaded = 0;
        int failed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation loc = entry.getKey();
            try {
                Material material = Material.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow(msg -> new IllegalStateException("Falha ao parsear material " + loc + ": " + msg));
                materials.put(material.id(), material);
                loaded++;
            } catch (Exception e) {
                LOGGER.error("Falha ao carregar material {}: {}", loc, e.getMessage());
                failed++;
            }
        }

        LOGGER.info("[HephaestusTools] Materiais carregados: {} OK, {} falhas", loaded, failed);
    }

    public MaterialStats getStats(MaterialId id) {
        Material mat = materials.get(id);
        return mat != null ? mat.headStats() : MaterialStats.EMPTY;
    }

    public MaterialStats getStatsForSlot(MaterialId id, int slot) {
        Material mat = materials.get(id);
        return mat != null ? mat.getStatsForSlot(slot) : MaterialStats.EMPTY;
    }

    @javax.annotation.Nullable
    public Material getMaterial(MaterialId id) {
        return materials.get(id);
    }

    public Collection<Material> getAllMaterials() {
        return Collections.unmodifiableCollection(materials.values());
    }

    public Map<MaterialId, Material> copyOfMap() {
        return Map.copyOf(materials);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        INSTANCE = new MaterialManager();
        event.addListener(INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncMaterialsPayload payload = new SyncMaterialsPayload(INSTANCE.copyOfMap());
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        } else {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }

    public static void receiveSync(Map<MaterialId, Material> synced) {
        INSTANCE.materials.clear();
        INSTANCE.materials.putAll(synced);
        LOGGER.info("[HephaestusTools] Materiais recebidos do servidor: {}", synced.size());
    }
}