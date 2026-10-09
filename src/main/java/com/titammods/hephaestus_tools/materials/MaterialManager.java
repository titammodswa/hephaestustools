package com.titammods.hephaestus_tools.materials;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.network.SyncMaterialsPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public final class MaterialManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialManager.class);
    private static final String FOLDER = "hephaestus_tools/materials";
    private static final String SUFFIX = ".json";
    private static final String CONDITIONS_KEY = "neoforge:conditions";

    public static final Identifier LISTENER_ID =
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "materials");

    private static final MaterialManager INSTANCE = new MaterialManager();

    private final Map<MaterialId, Material> materials = new LinkedHashMap<>();
    private final Map<MaterialId, Integer> fingerprints = new HashMap<>();

    private MaterialManager() {}

    public static MaterialManager getInstance() {
        return INSTANCE;
    }

    public MaterialStats getStats(MaterialId id) {
        Material mat = materials.get(id);
        return mat != null ? mat.headStats() : MaterialStats.EMPTY;
    }

    public MaterialStats getStatsForSlot(MaterialId id, int slot) {
        Material mat = materials.get(id);
        return mat != null ? mat.getStatsForSlot(slot) : MaterialStats.EMPTY;
    }

    public Material getMaterial(MaterialId id) {
        return materials.get(id);
    }

    public Collection<Material> getAllMaterials() {
        return Collections.unmodifiableCollection(materials.values());
    }

    public Map<MaterialId, Material> copyOfMap() {
        return Map.copyOf(materials);
    }

    public OptionalInt fingerprint(List<MaterialId> ids) {
        int hash = 1;
        for (MaterialId id : ids) {
            Integer part = fingerprints.get(id);
            if (part == null) return OptionalInt.empty();
            hash = 31 * hash + part;
        }
        return OptionalInt.of(hash);
    }

    private void replaceAll(Map<MaterialId, Material> source) {
        materials.clear();
        materials.putAll(source);
        fingerprints.clear();
        source.forEach((id, material) -> fingerprints.put(id, material.toString().hashCode()));
    }

    public static void receiveSync(Map<MaterialId, Material> synced) {
        INSTANCE.replaceAll(synced);
        LOGGER.info("[HephaestusTools] Materiais recebidos do servidor: {}", synced.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new Loader());
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

    public static final class Loader implements ResourceManagerReloadListener {

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            Map<MaterialId, Material> loaded = new LinkedHashMap<>();
            int failed = 0;
            int skipped = 0;

            for (Map.Entry<Identifier, Resource> entry :
                    manager.listResources(FOLDER, path -> path.getPath().endsWith(SUFFIX)).entrySet()) {
                Identifier file = entry.getKey();
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    if (!conditionsMet(json)) {
                        skipped++;
                        continue;
                    }
                    Material material = Material.CODEC.parse(JsonOps.INSTANCE, json)
                            .getOrThrow(msg -> new IllegalStateException(msg));
                    loaded.put(material.id(), material);
                } catch (Exception e) {
                    LOGGER.error("[HephaestusTools] Falha ao carregar material {}: {}", file, e.getMessage());
                    failed++;
                }
            }

            INSTANCE.replaceAll(loaded);
            LOGGER.info("[HephaestusTools] Materiais carregados: {} OK, {} falhas, {} ignorados por condicao", loaded.size(), failed, skipped);
        }

        private static boolean conditionsMet(JsonElement json) {
            if (!json.isJsonObject() || !json.getAsJsonObject().has(CONDITIONS_KEY)) return true;
            List<ICondition> conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, json.getAsJsonObject().get(CONDITIONS_KEY))
                    .getOrThrow(msg -> new IllegalStateException(msg));
            for (ICondition condition : conditions) {
                if (!condition.test(ICondition.IContext.EMPTY)) return false;
            }
            return true;
        }

        @Override
        public String getName() {
            return "HephaestusTools Materials";
        }
    }
}