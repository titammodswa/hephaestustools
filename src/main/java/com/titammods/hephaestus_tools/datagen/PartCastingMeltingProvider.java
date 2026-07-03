package com.titammods.hephaestus_tools.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PartCastingMeltingProvider implements DataProvider {

    private static final String TOOLS = "hephaestus_tools";
    private static final String FORGE = "hephaestus";

    private final PackOutput.PathProvider recipePath;

    public PartCastingMeltingProvider(PackOutput output) {
        this.recipePath = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    private record Part(String name, int cost) {}

    private record Metal(String name, int temp, boolean vanilla) {}

    private static final List<Part> PARTS = List.of(
            new Part("pick_head", 180),
            new Part("hammer_head", 720),
            new Part("small_axe_head", 180),
            new Part("broad_axe_head", 720),
            new Part("adze_head", 180),
            new Part("large_plate", 360),
            new Part("small_blade", 180),
            new Part("large_blade", 720),
            new Part("tool_handle", 90),
            new Part("tough_handle", 270),
            new Part("tool_binding", 90),
            new Part("tough_binding", 270)
    );

    private static final List<Metal> METALS = List.of(
            new Metal("iron", 900, true),
            new Metal("copper", 900, true),
            new Metal("aluminum", 660, false),
            new Metal("brass", 930, false),
            new Metal("bronze", 950, false),
            new Metal("constantan", 1220, false),
            new Metal("electrum", 1000, false),
            new Metal("enderium", 1450, false),
            new Metal("invar", 1420, false),
            new Metal("lead", 327, false),
            new Metal("lumium", 1000, false),
            new Metal("nickel", 1450, false),
            new Metal("osmium", 3000, false),
            new Metal("platinum", 1768, false),
            new Metal("signalum", 1000, false),
            new Metal("silver", 960, false),
            new Metal("tin", 230, false),
            new Metal("uranium", 1130, false),
            new Metal("zinc", 419, false)
    );

    private static int timeFor(int cost) {
        return 80 + (cost * 2) / 9;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Part part : PARTS) {
            futures.add(DataProvider.saveStable(cache, castCreation(part),
                    recipePath.json(id(TOOLS, "casting/casts/" + part.name() + "_cast"))));
            for (Metal metal : METALS) {
                String folder = metal.vanilla() ? metal.name() : "compat/" + metal.name();

                futures.add(DataProvider.saveStable(cache, casting(part, metal),
                        recipePath.json(id(TOOLS, "casting/" + folder + "/" + part.name()))));

                futures.add(DataProvider.saveStable(cache, melting(part, metal),
                        recipePath.json(id(TOOLS, "melting/" + folder + "/" + part.name()))));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private JsonObject casting(Part part, Metal metal) {
        JsonObject json = new JsonObject();
        addConditions(json, metal);
        json.addProperty("type", FORGE + ":casting_table");

        JsonObject cast = new JsonObject();
        cast.addProperty("item", TOOLS + ":" + part.name() + "_cast");
        json.add("cast", cast);

        json.addProperty("cast_consumed", false);
        json.add("fluid", fluid(FORGE + ":molten_" + metal.name(), part.cost()));
        json.addProperty("result", TOOLS + ":" + part.name());
        json.addProperty("count", 1);
        json.addProperty("cooling_time", timeFor(part.cost()));
        return json;
    }

    private JsonObject castCreation(Part part) {
        JsonObject json = new JsonObject();
        json.addProperty("type", FORGE + ":casting_table");

        JsonObject cast = new JsonObject();
        cast.addProperty("item", TOOLS + ":" + part.name());
        json.add("cast", cast);

        json.addProperty("cast_consumed", true);
        json.add("fluid", fluid(FORGE + ":molten_gold", 90));
        json.addProperty("result", TOOLS + ":" + part.name() + "_cast");
        json.addProperty("count", 1);
        json.addProperty("cooling_time", 100);
        return json;
    }

    private JsonObject melting(Part part, Metal metal) {
        JsonObject json = new JsonObject();
        addConditions(json, metal);
        json.addProperty("type", FORGE + ":melting");

        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("type", "neoforge:components");
        JsonObject components = new JsonObject();
        components.addProperty(TOOLS + ":part_material", TOOLS + ":" + metal.name());
        ingredient.add("components", components);
        ingredient.addProperty("items", TOOLS + ":" + part.name());
        ingredient.addProperty("strict", false);
        json.add("ingredient", ingredient);

        json.add("result", fluid(FORGE + ":molten_" + metal.name(), part.cost()));
        json.add("fuel", fuelForTemp(metal.temp()));
        json.addProperty("temperature", metal.temp());
        json.addProperty("time", timeFor(part.cost()));
        return json;
    }

    private static JsonObject fluid(String id, int amount) {
        JsonObject f = new JsonObject();
        f.addProperty("id", id);
        f.addProperty("amount", amount);
        return f;
    }

    private static JsonObject fuelForTemp(int temp) {
        return temp <= 1000 ? fluid("minecraft:lava", 50) : fluid(FORGE + ":molten_blaze", 50);
    }

    private static void addConditions(JsonObject json, Metal metal) {
        if (metal.vanilla()) return;
        JsonArray or = new JsonArray();
        or.add(modLoaded("alltheores"));
        or.add(modLoaded("ftbmaterials"));
        JsonObject orCond = new JsonObject();
        orCond.addProperty("type", "neoforge:or");
        orCond.add("values", or);
        JsonArray conditions = new JsonArray();
        conditions.add(orCond);
        json.add("neoforge:conditions", conditions);
    }

    private static JsonObject modLoaded(String modid) {
        JsonObject c = new JsonObject();
        c.addProperty("type", "neoforge:mod_loaded");
        c.addProperty("modid", modid);
        return c;
    }

    private static ResourceLocation id(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    @Override
    public String getName() {
        return "Hephaestus Tools - Part Casting & Melting";
    }
}