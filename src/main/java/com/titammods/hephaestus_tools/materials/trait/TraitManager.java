package com.titammods.hephaestus_tools.materials.trait;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.network.SyncTraitsPayload;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID)
public final class TraitManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraitManager.class);
    private static final FileToIdConverter FILES = FileToIdConverter.json("hephaestus_tools/traits");
    private static final String CONDITIONS_KEY = "neoforge:conditions";

    public static final Identifier LISTENER_ID =
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "traits");

    private static final TraitManager INSTANCE = new TraitManager();

    private volatile Map<Identifier, TraitDefinition> traits = Map.of();
    private volatile Map<Identifier, Integer> fingerprints = Map.of();

    private TraitManager() {}

    public static TraitManager getInstance() {
        return INSTANCE;
    }

    public Optional<TraitDefinition> get(Identifier id) {
        return Optional.ofNullable(traits.get(id));
    }

    public Map<Identifier, TraitDefinition> all() {
        return traits;
    }

    public int fingerprint(Identifier id) {
        return fingerprints.getOrDefault(id, 0);
    }

    private void replaceAll(Map<Identifier, TraitDefinition> source) {
        Map<Identifier, Integer> hashes = new HashMap<>();
        source.forEach((id, trait) -> hashes.put(id, trait.toString().hashCode()));
        traits = Collections.unmodifiableMap(new LinkedHashMap<>(source));
        fingerprints = Map.copyOf(hashes);
        ToolTraits.invalidate();
    }

    public static void receiveSync(List<SyncTraitsPayload.Entry> synced) {
        Map<Identifier, TraitDefinition> received = new LinkedHashMap<>();
        for (SyncTraitsPayload.Entry entry : synced) received.put(entry.id(), entry.definition());
        INSTANCE.replaceAll(received);
        LOGGER.info("[HephaestusTools] Traits received from the server: {}", received.size());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new Loader(event.getConditionContext()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncTraitsPayload payload = SyncTraitsPayload.of(INSTANCE.traits);
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
            Map<Identifier, TraitDefinition> loaded = new LinkedHashMap<>();
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
                    TraitDefinition trait = TraitDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                            .getOrThrow(msg -> new IllegalStateException(msg));
                    loaded.put(FILES.fileToId(file), trait);
                } catch (Exception e) {
                    LOGGER.error("[HephaestusTools] Could not load trait {}: {}", file, e.getMessage());
                    failed++;
                }
            }

            INSTANCE.replaceAll(loaded);
            LOGGER.info("[HephaestusTools] Traits loaded: {} OK, {} failed, {} skipped by condition", loaded.size(), failed, skipped);
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
            return "HephaestusTools Traits";
        }
    }
}
