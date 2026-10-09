package com.titammods.hephaestus_tools.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModItemModelProvider implements DataProvider {

    private static final String NS = HephaestusTools.MOD_ID;
    private static final String TOOL_TINT = NS + ":tool_material";
    private static final String PART_TINT = NS + ":part_material";
    private static final String MODIFIER_RENDERER = NS + ":tool_modifiers";
    private static final String WORKBENCH_STYLE = NS + ":workbench_style";

    private final PackOutput.PathProvider modelItemPath;
    private final PackOutput.PathProvider itemPath;

    public ModItemModelProvider(PackOutput output) {
        this.modelItemPath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
        this.itemPath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
    }

    private static final Map<String, List<String>> TOOL_LAYERS = new LinkedHashMap<>();

    static {
        TOOL_LAYERS.put("pickaxe", List.of(
                "item/tool/pickaxe/head", "item/tool/pickaxe/handle", "item/tool/pickaxe/binding"));
        TOOL_LAYERS.put("sledge_hammer", List.of(
                "item/tool/sledge_hammer/handle", "item/tool/sledge_hammer/head",
                "item/tool/sledge_hammer/front", "item/tool/sledge_hammer/back"));
        TOOL_LAYERS.put("vein_hammer", List.of(
                "item/tool/vein_hammer/handle", "item/tool/vein_hammer/head",
                "item/tool/vein_hammer/front", "item/tool/vein_hammer/grip"));
        TOOL_LAYERS.put("mattock", List.of(
                "item/tool/mattock/axe", "item/tool/pickaxe/handle", "item/tool/mattock/pick"));
        TOOL_LAYERS.put("excavator", List.of(
                "item/tool/excavator/head", "item/tool/excavator/grip",
                "item/tool/excavator/handle", "item/tool/excavator/binding"));
        TOOL_LAYERS.put("hand_axe", List.of(
                "item/tool/hand_axe/head", "item/tool/pickaxe/handle", "item/tool/hand_axe/binding"));
        TOOL_LAYERS.put("broad_axe", List.of(
                "item/tool/broad_axe/handle", "item/tool/broad_axe/blade",
                "item/tool/broad_axe/back", "item/tool/broad_axe/binding"));
        TOOL_LAYERS.put("kama", List.of(
                "item/tool/kama/head", "item/tool/pickaxe/handle", "item/tool/kama/binding"));
        TOOL_LAYERS.put("scythe", List.of(
                "item/tool/scythe/head", "item/tool/scythe/accessory",
                "item/tool/scythe/handle", "item/tool/scythe/binding"));
        TOOL_LAYERS.put("dagger", List.of(
                "item/tool/dagger/blade", "item/tool/dagger/guard",
                "item/tool/dagger/crossguard", "item/tool/dagger/handle"));
        TOOL_LAYERS.put("sword", List.of(
                "item/tool/sword/blade", "item/tool/sword/guard", "item/tool/sword/handle"));
        TOOL_LAYERS.put("cleaver", List.of(
                "item/tool/cleaver/head", "item/tool/cleaver/shield",
                "item/tool/cleaver/guard", "item/tool/cleaver/handle"));
    }

    private static final Map<String, List<String>> WORKBENCH_LAYERS = new LinkedHashMap<>();

    static {
        WORKBENCH_LAYERS.put("hand_axe", List.of(
                "item/tool/workbench/hand_axe/head", "item/tool/pickaxe/handle", "item/tool/workbench/hand_axe/binding"));
        WORKBENCH_LAYERS.put("cleaver", List.of(
                "item/tool/workbench/cleaver/head", "item/tool/cleaver/shield",
                "item/tool/cleaver/guard", "item/tool/cleaver/handle"));
    }

    private static final List<String> PARTS = List.of(
            "pick_head", "hammer_head", "small_axe_head", "broad_axe_head", "adze_head", "large_plate",
            "small_blade", "large_blade", "tool_handle", "tough_handle", "tool_binding", "tough_binding");

    private static final List<String> PLAIN_ITEMS = List.of(
            "pattern",
            "pick_head_cast", "hammer_head_cast", "small_axe_head_cast", "broad_axe_head_cast",
            "adze_head_cast", "large_plate_cast", "small_blade_cast", "large_blade_cast",
            "tool_handle_cast", "tough_handle_cast", "tool_binding_cast", "tough_binding_cast");

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : TOOL_LAYERS.entrySet()) {
            String tool = entry.getKey();
            List<String> layers = entry.getValue();

            futures.add(DataProvider.saveStable(cache, layeredToolModel(layers),
                    modelItemPath.json(id("tool/" + tool))));

            futures.add(DataProvider.saveStable(cache, overlayBaseModel(layers.get(0)),
                    modelItemPath.json(id("tool/" + tool + "_overlay"))));

            List<String> workbench = WORKBENCH_LAYERS.get(tool);
            if (workbench != null) {
                futures.add(DataProvider.saveStable(cache, layeredToolModel(workbench),
                        modelItemPath.json(id("tool/workbench/" + tool))));
                futures.add(DataProvider.saveStable(cache, styledToolClientItem(tool, layers.size()),
                        itemPath.json(id(tool))));
            } else {
                futures.add(DataProvider.saveStable(cache, toolClientItem(tool, layers.size()),
                        itemPath.json(id(tool))));
            }
        }

        for (String part : PARTS) {
            futures.add(DataProvider.saveStable(cache, partClientItem(part), itemPath.json(id(part))));
        }

        for (String plain : PLAIN_ITEMS) {
            futures.add(DataProvider.saveStable(cache, plainClientItem(plain), itemPath.json(id(plain))));
        }

        futures.add(DataProvider.saveStable(cache, plainClientItem("arsenal_table"),
                itemPath.json(id("arsenal_table"))));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private JsonObject layeredToolModel(List<String> layers) {
        JsonObject j = new JsonObject();
        j.addProperty("parent", "minecraft:item/handheld");
        JsonObject textures = new JsonObject();
        for (int i = 0; i < layers.size(); i++) {
            textures.addProperty("layer" + i, NS + ":" + layers.get(i));
        }
        j.add("textures", textures);
        return j;
    }

    private JsonObject overlayBaseModel(String particleLayer) {
        JsonObject j = new JsonObject();
        j.addProperty("parent", "minecraft:item/handheld");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", NS + ":" + particleLayer);
        j.add("textures", textures);
        return j;
    }

    private JsonObject styledToolClientItem(String tool, int layerCount) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "minecraft:condition");
        condition.addProperty("property", WORKBENCH_STYLE);
        condition.add("on_true", toolComposite("tool/workbench/" + tool, tool, layerCount));
        condition.add("on_false", toolComposite("tool/" + tool, tool, layerCount));
        JsonObject j = new JsonObject();
        j.add("model", condition);
        return j;
    }

    private JsonObject toolClientItem(String tool, int layerCount) {
        JsonObject j = new JsonObject();
        j.add("model", toolComposite("tool/" + tool, tool, layerCount));
        return j;
    }

    private JsonObject toolComposite(String modelPath, String tool, int layerCount) {
        JsonObject base = new JsonObject();
        base.addProperty("type", "minecraft:model");
        base.addProperty("model", NS + ":item/" + modelPath);
        JsonArray tints = new JsonArray();
        for (int i = 0; i < layerCount; i++) {
            JsonObject tint = new JsonObject();
            tint.addProperty("type", TOOL_TINT);
            tint.addProperty("index", i);
            tints.add(tint);
        }
        base.add("tints", tints);

        JsonObject overlays = new JsonObject();
        overlays.addProperty("type", "minecraft:special");
        overlays.addProperty("base", NS + ":item/tool/" + tool + "_overlay");
        JsonObject special = new JsonObject();
        special.addProperty("type", MODIFIER_RENDERER);
        overlays.add("model", special);

        JsonArray models = new JsonArray();
        models.add(base);
        models.add(overlays);

        JsonObject composite = new JsonObject();
        composite.addProperty("type", "minecraft:composite");
        composite.add("models", models);
        return composite;
    }

    private JsonObject partClientItem(String part) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", NS + ":item/" + part);
        JsonArray tints = new JsonArray();
        JsonObject tint = new JsonObject();
        tint.addProperty("type", PART_TINT);
        tint.addProperty("index", 0);
        tints.add(tint);
        model.add("tints", tints);

        JsonObject j = new JsonObject();
        j.add("model", model);
        return j;
    }

    private JsonObject plainClientItem(String name) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", NS + ":item/" + name);
        JsonObject j = new JsonObject();
        j.add("model", model);
        return j;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NS, path);
    }

    @Override
    public String getName() {
        return "Hephaestus Tools - Item Models";
    }
}