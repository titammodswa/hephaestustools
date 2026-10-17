package com.titammods.hephaestus_tools.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.titammods.hephaestus_tools.materials.tier.BuiltInTiers;
import com.titammods.hephaestus_tools.materials.tier.TierDefinition;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TierProvider implements DataProvider {

    private final PackOutput.PathProvider tierPath;

    public TierProvider(PackOutput output) {
        this.tierPath = output.createPathProvider(PackOutput.Target.DATA_PACK, "hephaestus_tools/tiers");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Map.Entry<Identifier, TierDefinition> entry : BuiltInTiers.definitions().entrySet()) {
            JsonElement json = TierDefinition.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                    .getOrThrow(message -> new IllegalStateException(entry.getKey() + ": " + message));
            futures.add(DataProvider.saveStable(cache, json, tierPath.json(entry.getKey())));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Hephaestus Tools - Tiers";
    }
}
