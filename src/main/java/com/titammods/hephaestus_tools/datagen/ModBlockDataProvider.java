package com.titammods.hephaestus_tools.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModBlockDataProvider implements DataProvider {
    private static final String TABLE = HephaestusTools.MOD_ID + ":arsenal_table";
    private final PackOutput.PathProvider lootPath;
    private final PackOutput.PathProvider tagPath;

    public ModBlockDataProvider(PackOutput output) {
        lootPath = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
        tagPath = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/block");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", TABLE);
        JsonArray entries = new JsonArray();
        entries.add(entry);

        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(condition);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", entries);
        pool.add("conditions", conditions);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject loot = new JsonObject();
        loot.addProperty("type", "minecraft:block");
        loot.add("pools", pools);

        JsonArray values = new JsonArray();
        values.add(TABLE);
        JsonObject tag = new JsonObject();
        tag.add("values", values);

        return CompletableFuture.allOf(
                DataProvider.saveStable(cache, loot, lootPath.json(Identifier.parse(TABLE))),
                DataProvider.saveStable(cache, tag, tagPath.json(Identifier.withDefaultNamespace("mineable/pickaxe"))));
    }

    @Override
    public String getName() {
        return "Hephaestus Tools - Block Loot and Tags";
    }
}
