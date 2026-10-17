package com.titammods.hephaestus_tools.materials.tier;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.network.SyncTiersPayload;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public final class TierManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TierManager.class);
    private static final FileToIdConverter FILES = FileToIdConverter.json("hephaestus_tools/tiers");
    private static final String CONDITIONS_KEY = "neoforge:conditions";

    public static final Identifier LISTENER_ID =
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "tiers");

    private static final TierManager INSTANCE = new TierManager();

    private volatile Map<Identifier, TierDefinition> tiers = Map.of();
    private volatile int fingerprint = 1;

    private TierManager() {}

    public static TierManager getInstance() {
        return INSTANCE;
    }

    public Optional<TierDefinition> get(Identifier id) {
        return Optional.ofNullable(tiers.get(id));
    }

    public Map<Identifier, TierDefinition> all() {
        return tiers;
    }

    public int fingerprint() {
        return fingerprint;
    }

    private void replaceAll(Map<Identifier, TierDefinition> source) {
        int hash = 1;
        for (Map.Entry<Identifier, TierDefinition> entry : source.entrySet()) {
            hash = 31 * hash + entry.getKey().hashCode();
            hash = 31 * hash + entry.getValue().toString().hashCode();
        }
        tiers = Collections.unmodifiableMap(new LinkedHashMap<>(source));
        fingerprint = hash;
    }

    public static void receiveSync(List<SyncTiersPayload.Entry> synced) {
        Map<Identifier, TierDefinition> received = new LinkedHashMap<>();
        for (SyncTiersPayload.Entry entry : synced) received.put(entry.id(), entry.definition());
        INSTANCE.replaceAll(received);
        LOGGER.info("[HephaestusTools] Tiers received from the server: {}", received.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new Loader(event.getConditionContext()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncTiersPayload payload = SyncTiersPayload.of(INSTANCE.tiers);
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        } else {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }

    public static final class Loader implements ResourceManagerReloadListener {

        private final ICondition.IContext context;

        public Loader(ICondition.IContext context) {
            this.context = context;
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            Map<Identifier, TierDefinition> loaded = new LinkedHashMap<>();
            int failed = 0;
            int skipped = 0;

            for (Map.Entry<Identifier, Resource> entry : FILES.listMatchingResources(manager).entrySet()) {
                Identifier file = entry.getKey();
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    if (!conditionsMet(json)) {
                        skipped++;
                        continue;
                    }
                    TierDefinition tier = TierDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                            .getOrThrow(msg -> new IllegalStateException(msg));
                    loaded.put(FILES.fileToId(file), tier);
                } catch (Exception e) {
                    LOGGER.error("[HephaestusTools] Could not load tier {}: {}", file, e.getMessage());
                    failed++;
                }
            }

            INSTANCE.replaceAll(loaded);
            LOGGER.info("[HephaestusTools] Tiers loaded: {} OK, {} failed, {} skipped by condition", loaded.size(), failed, skipped);
        }

        private boolean conditionsMet(JsonElement json) {
            if (!json.isJsonObject() || !json.getAsJsonObject().has(CONDITIONS_KEY)) return true;
            List<ICondition> conditions = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, json.getAsJsonObject().get(CONDITIONS_KEY))
                    .getOrThrow(msg -> new IllegalStateException(msg));
            for (ICondition condition : conditions) {
                if (!condition.test(context)) return false;
            }
            return true;
        }

        @Override
        public String getName() {
            return "HephaestusTools Tiers";
        }
    }
}
